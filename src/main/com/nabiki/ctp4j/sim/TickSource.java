/*
 * Copyright (c) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * Licensed under the  GNU Affero General Public License v3.0 and you may not use
 * this file except in compliance with the  License. You may obtain a copy of the
 * License at
 *
 *                    https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Permission is hereby  granted, free of charge, to any  person obtaining a copy
 * of this software and associated  documentation files (the "Software"), to deal
 * in the Software  without restriction, including without  limitation the rights
 * to  use, copy,  modify, merge,  publish, distribute,  sublicense, and/or  sell
 * copies  of  the Software,  and  to  permit persons  to  whom  the Software  is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE  IS PROVIDED "AS  IS", WITHOUT WARRANTY  OF ANY KIND,  EXPRESS OR
 * IMPLIED,  INCLUDING BUT  NOT  LIMITED TO  THE  WARRANTIES OF  MERCHANTABILITY,
 * FITNESS FOR  A PARTICULAR PURPOSE AND  NONINFRINGEMENT. IN NO EVENT  SHALL THE
 * AUTHORS  OR COPYRIGHT  HOLDERS  BE  LIABLE FOR  ANY  CLAIM,  DAMAGES OR  OTHER
 * LIABILITY, WHETHER IN AN ACTION OF  CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE  OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nabiki.ctp4j.sim;

import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;
import com.nabiki.ctp4j.md.CThostFtdcMdSpi;

import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class TickSource extends TimerTask {
    class SpiDaemon implements Runnable {
        private final CThostFtdcMdSpi spi;
        private final BlockingQueue<CThostFtdcDepthMarketDataField> depths;
        private boolean stopped = Boolean.FALSE;

        SpiDaemon(CThostFtdcMdSpi spi) {
            this.spi = spi;
            this.depths = new LinkedBlockingQueue<>();
        }

        boolean add(CThostFtdcDepthMarketDataField depth) {
            return this.depths.offer(depth);
        }

        void stop() {
            this.stopped = true;
        }

        @Override
        public void run() {
            while (!this.stopped) {
                try {
                    var depth = this.depths.poll(1, TimeUnit.DAYS);
                    if (depth != null && !this.stopped)
                        this.spi.OnRtnDepthMarketData(depth);
                } catch (Throwable ignored) {}
            }
        }
    }

    private final static TickSource source = new TickSource();

    private final Timer timer = new Timer();
    private final Random random = new Random(TickSource.class.hashCode());
    private final Map<String, TickBook> books = new ConcurrentHashMap<>();
    private final Map<CThostFtdcMdSpi, SpiDaemon> daemons
            = new ConcurrentHashMap<>();
    private final ExecutorService threads = Executors.newCachedThreadPool();

    TickSource() {
        var delay = 500 - System.currentTimeMillis() % 500;
        this.timer.scheduleAtFixedRate(this, delay, 500);
    }

    public static TickSource getTickSource() {
        return source;
    }

    public void initialize(CThostFtdcDepthMarketDataField depth) {
        if (depth == null)
            throw new NullPointerException("depth null");
        if (this.books.containsKey(depth.InstrumentID))
            throw new IllegalStateException("duplicated books");
        var inst = CommonData.getInstrument(depth.InstrumentID);
        if (inst == null)
            throw new IllegalStateException("instrument null");
        this.books.put(depth.InstrumentID, new TickBook(depth, inst.PriceTick));
    }

    public void addSPI(CThostFtdcMdSpi spi) {
        if (this.daemons.containsKey(spi))
            throw new IllegalArgumentException("duplicated spi");
        var daemon = new SpiDaemon(spi);
        this.daemons.put(spi, daemon);
        this.threads.execute(daemon);
    }

    public void removeSPI(CThostFtdcMdSpi spi) {
        var daemon = this.daemons.get(spi);
        if (daemon != null) {
            daemon.stop();
            this.daemons.remove(spi);
        }
    }

    @Override
    public void run() {
        for (var book : this.books.values()) {
            // There is a chance that it doesn't emit the depth.
            if (this.random.nextDouble() <= 0.5)
                continue;
            // There is a very small chance that the market changes state.
            if (this.random.nextDouble() < 0.0001)
                book.setBuyChance(newBuyChance(book.getBuyChance()));
            // Refresh depth.
            var depth = book.refresh();
            for (var daemon : this.daemons.values())
                daemon.add(depth);
        }
    }

    private double newBuyChance(double oldChance) {
        var f = this.random.nextDouble() / 20.D;
        if (oldChance > 0.5D)
            return 0.5D - f;
        else if (oldChance < 0.5D)
            return 0.5D + f;
        else
            return 0.5D + f * (this.random.nextInt(3) - 1);
    }
}
