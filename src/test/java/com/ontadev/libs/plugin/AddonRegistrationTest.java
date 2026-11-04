package com.ontadev.libs.plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.service.AddonProvider;
import net.luckperms.api.LuckPerms;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class AddonRegistrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void libraryHookWorksWithoutProtocolLibClasses() throws Exception {
        String libraryName = OntaDev_Libs.class.getName();
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.comphenix.protocol.")) throw new ClassNotFoundException(name);
                if (!name.equals(libraryName)) return super.loadClass(name, resolve);
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (java.io.InputStream in = getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = in.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (java.io.IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        Class<? extends OntaDev_Template> type = loader.loadClass(libraryName).asSubclass(OntaDev_Template.class);
        OntaDev_Template library = mock(type, CALLS_REAL_METHODS);
        Server server = mock(Server.class);
        when(library.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        ServicesManager services = mock(ServicesManager.class);
        RegisteredServiceProvider<LuckPerms> registration = mock(RegisteredServiceProvider.class);
        when(services.getRegistration(LuckPerms.class)).thenReturn(registration);
        LuckPerms api = mock(LuckPerms.class);
        when(registration.getProvider()).thenReturn(api);
        AddonProvider addons = new AddonProvider(services);
        java.lang.reflect.Method hook = type.getDeclaredMethod("registerAddons", AddonProvider.class);
        hook.setAccessible(true);
        hook.invoke(library, addons);
        PluginIoC ioc = mock(PluginIoC.class);
        addons.registerIn(ioc);
        verify(ioc).registerInstance(LuckPerms.class, api);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registersBuiltInApisThroughLibraryHook() {
        OntaDev_Libs library = mock(OntaDev_Libs.class);
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        ServicesManager services = mock(ServicesManager.class);
        RegisteredServiceProvider<LuckPerms> registration = mock(RegisteredServiceProvider.class);
        LuckPerms luckPerms = mock(LuckPerms.class);
        ProtocolManager protocol = mock(ProtocolManager.class);
        when(library.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(plugins);
        when(plugins.isPluginEnabled("ProtocolLib")).thenReturn(true);
        when(services.getRegistration(LuckPerms.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(luckPerms);
        AddonProvider addons = new AddonProvider(services);
        doCallRealMethod().when(library).registerAddons(addons);
        try (MockedStatic<ProtocolLibrary> api = mockStatic(ProtocolLibrary.class)) {
            api.when(ProtocolLibrary::getProtocolManager).thenReturn(protocol);
            library.registerAddons(addons);
            library.registerAddons(addons);
            api.verify(ProtocolLibrary::getProtocolManager, times(1));
        }
        PluginIoC ioc = mock(PluginIoC.class);
        addons.registerIn(ioc);
        verify(ioc).registerInstance(LuckPerms.class, luckPerms);
        verify(ioc).registerInstance(ProtocolManager.class, protocol);
    }
}
