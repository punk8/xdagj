/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.xdag.mine.miner;

import io.xdag.mine.MinerChannel;
import io.xdag.utils.BytesUtils;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.xdag.utils.WalletUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tuweni.bytes.Bytes32;

@Slf4j
public class Miner {

    /**
     * 保存这个矿工的地址
     */
    private final Bytes32 addressHash;

    @Getter
    private final byte[] addressHashByte;
    /**
     * 相同账户地址的channel数量
     */
    private final AtomicInteger connChannelCounts = new AtomicInteger(0);
    /**
     * 存放的是连续16个任务本地计算的最大难度 每一轮放的都是最小hash 计算出来的diffs
     */
    private final List<Double> maxDiffs = new CopyOnWriteArrayList<>();

    /**
     * 保存的是这个矿工对应的channel
     */
    private final Map<InetSocketAddress, MinerChannel> channels = new ConcurrentHashMap<>();

    protected int boundedTaskCounter;
    /**
     * 记录收到任务的时间
     */
    private long taskTime;
    @Getter
    @Setter
    /* 记录任务索引 * */
    private long taskIndex;
    /**
     * 记录的是当前任务所有难度之和，每当接收到一个新的nonce 会更新这个
     */
    private double prevDiff;
    /**
     * 记录prevDiff的次数 实际上类似于进行了多少次计算
     */
    private int prevDiffCounts;
    /**
     * 记录这个矿工的状态
     */
    private MinerStates minerStates;
    /**
     * 类似于id 也是保存的nonce +hasholow的值
     */
    @Getter
    @Setter
    private Bytes32 nonce;
    /**
     * 记录上一轮任务中最小的hash
     */
    private Bytes32 lastMinHash;
    /**
     * 将hash转换后的难度 可以认为是算力
     */
    private double meanLogDiff;
    private Date registeredTime;

    public Miner(Bytes32 addressHash) {
        log.debug("init the new miner:{}", addressHash.toHexString());
        this.addressHash = addressHash;
        addressHashByte = BytesUtils.byte32ToArray(addressHash.mutableCopy());
        this.minerStates = MinerStates.MINER_UNKNOWN;
        this.taskTime = 0;
        this.meanLogDiff = 0.0;
        this.registeredTime = Calendar.getInstance().getTime();
        boundedTaskCounter = 0;
        // 容器的初始化
        for (int i = 0; i < 16; i++) {
            maxDiffs.add(0.0);
        }
    }

    public Bytes32 getAddressHash() {
        return this.addressHash;
    }

    public int getConnChannelCounts() {
        return connChannelCounts.get();
    }

    public MinerStates getMinerStates() {
        return this.minerStates;
    }

    public void setMinerStates(MinerStates states) {
        this.minerStates = states;
    }

    /**
     * 判断这个miner 是不是可以被移除
     * 没有矿机接入
     * 状态等于归档
     * maxdiff 全部为0
     */
    public boolean canRemove() {
        if (minerStates == MinerStates.MINER_ARCHIVE && channels.size() == 0) {
            for (Double maxDiff : maxDiffs) {
                if (maxDiff.compareTo((double) 0) > 0) {
                    return false;
                }
            }
            log.debug("remove Miner: {}", WalletUtils.toBase58(addressHashByte));
            return true;
        } else {
            return false;
        }
    }

    public long getTaskTime() {
        return this.taskTime;
    }

    public void setTaskTime(long time) {
        this.taskTime = time;
    }

    public double getMaxDiffs(int index) {
        return maxDiffs.get(index);
    }

    public void addPrevDiff(double i) {
        prevDiff += i;
    }

    public void addPrevDiffCounts() {
        this.prevDiffCounts++;
    }

    public void setMaxDiffs(int index, double diff) {
        maxDiffs.set(index, diff);
    }

    public double getPrevDiff() {
        return prevDiff;
    }

    public void setPrevDiff(double i) {
        this.prevDiff = i;
    }

    public int getPrevDiffCounts() {
        return prevDiffCounts;
    }

    public void setPrevDiffCounts(int i) {
        this.prevDiffCounts = i;
    }

    public Date getRegTime() {
        return registeredTime;
    }

    public Map<InetSocketAddress, MinerChannel> getChannels() {
        return channels;
    }

    public double getMeanLogDiff() {
        return this.meanLogDiff;
    }

    public void setMeanLogDiff(double meanLogDiff) {
        this.meanLogDiff = meanLogDiff;
    }

    public Bytes32 getLastMinHash() {
        return lastMinHash;
    }

    public void setLastMinHash(Bytes32 lastMinHash) {
        this.lastMinHash = lastMinHash;
    }

    public int getBoundedTaskCounter() {
        return boundedTaskCounter;
    }

    public void addBoundedTaskCounter() {
        this.boundedTaskCounter++;
    }

    public void putChannel(InetSocketAddress inetSocketAddress, MinerChannel channel) {
        this.channels.put(inetSocketAddress, channel);
        connChannelCounts.incrementAndGet();
    }

    public void removeChannel(InetSocketAddress address) {
        this.channels.remove(address);
        connChannelCounts.getAndDecrement();
    }
}
