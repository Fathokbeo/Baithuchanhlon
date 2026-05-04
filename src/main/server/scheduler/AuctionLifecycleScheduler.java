package main.server.scheduler;

import main.server.controller.ServerRequestController;
import main.server.service.AuctionService;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AuctionLifecycleScheduler implements AutoCloseable {
    private final AuctionService auctionService;
    private final ServerRequestController controller;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public AuctionLifecycleScheduler(AuctionService auctionService, ServerRequestController controller) {
        this.auctionService = auctionService;
        this.controller = controller;
    }

    public void start() {
        executorService.scheduleAtFixedRate(() -> auctionService.processLifecycleTick(LocalDateTime.now())
                .forEach(auction -> controller.broadcastAuctionChange(auction, "LIFECYCLE_TICK")), 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
