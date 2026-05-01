package com.mycompany.action;

import com.mycompany.models.PhienDauGia;
import com.mycompany.models.TrangThaiPhien;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.*;

public class PhienDauGiaScheduler {
    private static volatile PhienDauGiaScheduler instance;

    private PhienDauGiaScheduler() {}

    public static PhienDauGiaScheduler getInstance() {
        if (instance == null) {
            synchronized (PhienDauGiaScheduler.class) {
                if (instance == null) {
                    instance = new PhienDauGiaScheduler();
                }
            }
        }
        return instance;
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3, runnable -> {
        Thread task = new Thread(runnable);
        task.setDaemon(false);
        return task;
    });

    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final PhienDauGiaService phienDauGiaService = PhienDauGiaService.getInstance();

    public void lenLichDongPhien(PhienDauGia phien) {
        String maPhien = phien.getMaPhien();
        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getThoiGianKetThuc()).toSeconds();
        if (delay <= 0) return;

        huyPhien(phien);

        ScheduledFuture<?> future = executor.schedule(() -> dongPhien(phien), delay, TimeUnit.SECONDS);
        scheduledFutures.put(maPhien, future);
    }
    public void huyPhien(PhienDauGia phien) {
        String maPhien = phien.getMaPhien();

        ScheduledFuture<?> future = scheduledFutures.remove(maPhien);
        if(future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
    public void dongPhien(PhienDauGia phien) {
        String maPhien = phien.getMaPhien();

        phienDauGiaService.dongPhien(phien, TrangThaiPhien.DA_THANH_TOAN);
        scheduledFutures.remove(maPhien);
    }
}