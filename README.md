# OntaDev_Libs

Библиотека для Paper/Bukkit-плагинов: DI-контейнер с автосканированием, ORM на JDBI с поддержкой 4 диалектов, система меню с анимацией через задачи, сообщения на MiniMessage, YAML-конфиги.

Распространяется как отдельный Bukkit-плагин, а не как обычная Maven-библиотека - остальные плагины подключаются к нему как к рантайм-зависимости и получают DI, ORM и меню через общий `PluginIoC`.

## Требования

- Java 11+
- Paper/Spigot 1.16.5+ (`api-version: 1.16`)
- Maven

## Установка

`plugin.yml` вашего плагина:

```yaml
depend: [OntaDev_Libs]
```

Для компиляции нужен jar библиотеки (GitHub Releases) в classpath как `provided`-зависимость:

```bash
mvn install:install-file -Dfile=OntaDev_Libs.jar -DgroupId=com.ontadev -DartifactId=libs -Dversion=X.Y.Z -Dpackaging=jar
```

```xml
<dependency>
    <groupId>com.ontadev</groupId>
    <artifactId>libs</artifactId>
    <version>X.Y.Z</version>
    <scope>provided</scope>
</dependency>
```

## Быстрый старт

Главный класс плагина наследует `OntaDev_Template`:

```java
public final class MyPlugin extends OntaDev_Template {
    @Override
    public void onPluginEnable(PluginIoC pluginIoC) {
        // плагин готов, IoC-контейнер уже инициализирован
    }
}
```

При старте `OntaDev_Template` создаёт `PluginIoC`, сканирует все классы плагина (`AnnotationScanner`, на основе ClassGraph) и создаёт компоненты, помеченные стереотип-аннотациями. Никакой ручной регистрации бинов не нужно - достаточно аннотации на классе.

## IoC-контейнер

### Стереотипы

Класс становится компонентом (создаётся и живёт в контейнере), если помечен одной из аннотаций:

| Аннотация | Назначение |
|---|---|
| `@Service` | обычный сервис |
| `@Component` | компонент общего назначения |
| `@Repository` | компонент уровня доступа к данным (не путать с ORM-репозиториями ниже) |
| `@Config` | класс настроек, автоматически загружается через `YamlConfigLoader` перед созданием |
| `@Command` | наследник `AbstractCommand`, автоматически регистрируется в Bukkit |
| `@Menu` | наследник `AbstractMenu`, автоматически регистрируется в `MenuManager` |
| `@AutoListener` | наследник `Listener`, автоматически регистрируется через `Bukkit.getPluginManager()` |

```java
@Service
public class RewardService {
    private final EconomyRepository economy;

    public RewardService(EconomyRepository economy) {
        this.economy = economy;
    }
}
```

Порядок инициализации компонентов вычисляется автоматически: `IoCContainer` строит граф зависимостей по конструкторам и топологически сортирует его. Циклическая зависимость между компонентами приводит к `IllegalStateException` при старте - если цикл нужен намеренно, разрывайте его через `Provider<T>` (см. ниже).

### @Inject

`@Inject` работает в двух местах:

Выбор конструктора, если их несколько:

```java
public class RewardService {
    public RewardService() { }

    @Inject
    public RewardService(EconomyRepository economy) { }
}
```

Инъекция поля (нужен зарегистрированный `InjectFieldHandler`, он уже стоит по умолчанию):

```java
@Service
public class RewardService {
    @Inject
    private EconomyRepository economy;
}
```

### Provider\<T\> (ленивая загрузка)

Параметр или поле типа `Provider<X>` не создаёт `X` сразу - только при вызове `.get()`. Это разрывает циклы зависимостей и откладывает дорогое создание объекта до реального использования:

```java
@Service
public class ReportGenerator {
    private final Provider<HeavyPdfEngine> engine;

    public ReportGenerator(Provider<HeavyPdfEngine> engine) {
        this.engine = engine;
    }

    public void generate() {
        engine.get().render(); // HeavyPdfEngine создаётся именно тут
    }
}
```

`Provider<X>` не участвует в графе зависимостей - раз он не форсирует создание `X`, он и не должен навязывать порядок инициализации.

### Ручное получение компонентов

```java
MyService service = pluginIoC.get(MyService.class);
```

## ORM

Собственный слой поверх JDBI Core (не SqlObject): аннотации сущностей, автогенерация схемы, `@Query`/`@Modifying` для произвольного SQL, `returnable` `save()`. Поддерживаемые диалекты: MySQL, PostgreSQL, SQLite, H2.

### Сущность

```java
@Getter
@Setter
@Entity
@Table("users")
public class User {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @Column(nullable = false, unique = true)
    private UUID externalId;

    @Column(type = "TEXT")
    private Map<String, String> metadata;
}
```

- `@Id` - первичный ключ. Тип поля обязан быть wrapper-классом (`Long`, не `long`) - иначе `save()` не отличит "id не задан" от "id равен 0".
- `@GeneratedValue` - автоинкремент. Без него `save()` ждёт, что вы задали id сами (бизнес-ключ, UUID и т.п.).
- `@Column(type = "...")` - явный SQL-тип, обязателен для полей, чей Java-тип не покрыт автоопределением (см. адаптеры типов ниже).

Таблица создаётся автоматически при первом обращении к репозиторию - под конкретный диалект (`Dialect`), определяемый по JDBC URL.

### Репозиторий

Интерфейс, наследующий `OrmRepository<T, ID>`, находится автосканированием и превращается в proxy-реализацию без единой строчки кода:

```java
public interface UserRepository extends OrmRepository<User, Long> {

    @Query("SELECT * FROM users WHERE name = ?")
    Optional<User> findByName(String name);

    @Query("SELECT * FROM users WHERE name = ?")
    List<User> findAllByName(String name);

    @Modifying("DELETE FROM users WHERE id = ?")
    void deleteById(Long id);
}
```

Правила биндинга параметров:

- SQL с `?` - позиционные аргументы, по порядку.
- SQL с `:name` - именованные (`org.jdbi.v3.sqlobject.customizer.Bind`), имя параметра метода читается через `-parameters` (уже включено в `pom.xml`) либо задаётся явно через `@Bind("name")`.

Возвращаемые типы `@Query`:

- `T` - бросает `NoResultException`, если строки нет.
- `Optional<T>` - пустой `Optional`, если строки нет.
- `List<T>` / `Iterable<T>` - список найденных строк.
- Любой из вариантов можно обернуть в `CompletableFuture<...>` вместе с `@Async` - выполнится на отдельном пуле потоков.

`save()` из `OrmRepository<T, ID>` уже реализован и ничего писать не нужно:

```java
User user = new User();
user.setName("Notch");

User saved = repository.save(user); // id проставлен автоматически
```

Логика `save()`:

- id не задан и сущность с `@GeneratedValue` - `INSERT`, id считывается из `RETURN_GENERATED_KEYS` и проставляется в сущность.
- id задан и сущность с `@GeneratedValue` - `UPDATE` (предполагается, что запись уже существует).
- id задан и сущность без `@GeneratedValue` (бизнес-ключ) - проверка существования, затем `INSERT` либо `UPDATE`.

### Адаптеры типов

JDBI Core сам умеет `String`, числа, `boolean`, `BigDecimal`, дату-время, `enum`. Библиотека добавляет:

- `UuidTypeAdapter` - `UUID` всегда пишется и читается как строка (`VARCHAR(36)`), а не через `setObject`, одинаково на всех 4 диалектах.
- `JsonTypeAdapter` - любой Java-тип, не покрытый встроенными обработчиками (списки, `Map`, вложенные DTO), сериализуется в JSON через Gson. Требует явного `@Column(type = "TEXT")`.

## Система меню

### Меню

```java
@Menu
public class ShopMenu extends AbstractMenu {

    @Override
    public Message title(PlayerSnapshot snapshot) {
        return new Message("Магазин");
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public Map<Integer, ItemModel> staticItems() {
        Map<Integer, ItemModel> items = new HashMap<>();

        ItemModel diamond = ItemModel.builder()
                .material(Material.DIAMOND)
                .name(new Message("Алмаз"))
                .build();
        diamond.onInteraction((player, event) -> player.sendMessage("Клик!"), InteractionType.LEFT_CLICK);

        items.put(13, diamond);
        return items;
    }
}
```

- `staticItems()` - предметы, общие для всех зрителей меню. Рассчитываются и кэшируются один раз при регистрации меню.
- `dynamicItems(PlayerSnapshot)` - предметы, зависящие от конкретного игрока (баланс, кулдауны и т.п.). Слот из `dynamicItems` перекрывает тот же слот из `staticItems`.
- `refreshPeriod()` - если > 0, `dynamicItems` пересчитывается для сессии каждые N тиков.
- `tasks()` - список `MenuTask`, запускаемых при открытии меню и отменяемых при закрытии.

Открытие/закрытие:

```java
menu.open(player);
menu.close(snapshot);
```

### Клики по предметам

Обработчики регистрируются на уже собранном `ItemModel` (это не часть билдера) и возвращают `this` для цепочки вызовов:

```java
ItemModel emerald = ItemModel.builder()
        .material(Material.EMERALD)
        .cancelClick(true) // по умолчанию true - предмет нельзя вытащить из меню
        .build();

emerald.onInteraction((player, event) -> { /* обычный клик */ }, InteractionType.LEFT_CLICK, InteractionType.RIGHT_CLICK)
        .onAnyInteraction((player, event) -> { /* всё остальное */ });
```

### Отложенные задачи (`MenuTask`)

```java
// раз в секунду (20 тиков) меняет предмет в слоте 0 - например, кадр анимации
MenuTask.every(20L, session -> session.getInventory().setItem(0, new ItemStack(Material.TORCH)));

// один раз через 5 секунд закрывает меню
MenuTask.once(100L, session -> session.getAbstractMenu().close(session.getPlayerSnapshot()));
```

Обе задачи выполняются через общий тикер `MenuManagerImpl` (раз в тик), а не через отдельный `BukkitRunnable` на задачу, и автоматически снимаются при закрытии меню.

## Сообщения

`Message` хранит текст с MiniMessage-разметкой, `Placeholders` - подстановки:

```java
Message message = new Message("<gold>Привет, <name>!</gold>");

message.send(player, Placeholders.of("name", player.getName()));
```

- `Message` умеет строиться из `String...`, `List<String>`, готовых `Component`.
- Плейсхолдер в тексте - `<key>`, подставляется через `Placeholders.of(key, values...)` или `.add(key, values...)`.
- Отправка: `send(Player)`, `send(CommandSender)`, `send(UUID)`, `sendActionBar(Audience)` - все уходят на главный поток автоматически, даже если вызваны асинхронно.

## Конфиги

```java
@Getter
@Setter
@Config
public class MySettings extends YamlConfig {
    private int maxHomes = 5;
    private String prefix = "&7[Plugin]";

    @Override
    public String getFileName() {
        return "settings";
    }
}
```

Помеченный `@Config` класс - обычный компонент: создаётся автоматически при старте, файл `settings.yml` читается или создаётся с значениями по умолчанию. Изменения сохраняются вызовом `save()`:

```java
settings.setMaxHomes(10);
settings.save();
```

Новые поля, добавленные в класс после первого запуска, автоматически дописываются в существующий YAML-файл при следующей загрузке.

## Сборка

```bash
mvn clean package
```

Собирает shaded jar (`maven-shade-plugin`) со всеми зависимостями, кроме `provided` (Paper API, Adventure, LuckPerms - их даёт сервер). Тесты гоняются на реальных H2, SQLite и, если доступны без Docker, embedded Postgres/MariaDB - при недоступности движка на текущей платформе такие тесты сами помечаются skipped, а не валят сборку.

CI: `.github/workflows/build.yml`, self-hosted раннер, JDK 17. При пуше в `master` собранный jar публикуется в GitHub Releases под версией из `pom.xml`.
