// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.menu.task;

import com.ontadev.libs.menu.MenuSession;
import lombok.Getter;
import lombok.Setter;

public class MenuTaskState {
    @Getter
    private final MenuTask task;
    @Getter
    private final MenuSession session;

    @Getter
    @Setter
    private long nextRun;

    public MenuTaskState(MenuTask task, MenuSession session, long currentTick) {
        this.task = task;
        this.session = session;
        this.nextRun = currentTick + task.getDelay();
    }

    public void scheduleNext(){
        this.nextRun+= task.getPeriod();
    }

    public void run(MenuSession session){
        task.getConsumer().accept(session);
    }
}
