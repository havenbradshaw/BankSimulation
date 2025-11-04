Diagnostic Report — BankSimulation Concurrency Bottleneck

Date: 2025-11-01

Summary:
`BankSimulation` simulates multiple threads withdrawing from a shared bank account. The program exhibits nondeterministic final balances across runs and occasional negative or skipped balances. The core issue is a race condition: the withdraw operation executes a non-atomic check-then-update on a shared `int balance` while multiple threads run concurrently.

Evidence:
Representative output from an original (unsynchronized) run (see `before_run.txt`):

```
=== Starting transactions with balance = $1000 ===
Diana withdrew $50 | old balance = 1000 -> new balance = 950
Charlie withdrew $50 | old balance = 1000 -> new balance = 950
Bob withdrew $50 | old balance = 1000 -> new balance = 950
ATM-Kiosk withdrew $20 | old balance = 1000 -> new balance = 980
Alice withdrew $50 | old balance = 1000 -> new balance = 950
...
```

These lines show multiple threads reading the same `old balance` value (1000) and computing new balances independently, demonstrating lost updates.

Excerpt of the problematic code (check-then-act):
```java
// BankAccount.withdraw (original)
if (balance >= amount) {
    int oldBalance = balance;
    // fakeWork();
    int newBalance = oldBalance - amount;
    balance = newBalance;
}
```
Why this is a bug: two threads can both test `balance >= amount` true, then both compute `newBalance` from the same `oldBalance` and write it back — one update overwrites the other, producing lost withdrawals and inconsistent final totals.

When it occurs:
- Timing windows where `fakeWork()` or sleeps allow interleaving between the check and update cause the issue. The more threads and the longer the artificial processing (or the faster the hammers), the more likely the race shows up.

Proposed Fix #1 — Synchronized critical section (implemented):
- Change the withdraw method to be `synchronized` so that the entire check-modify-write sequence is atomic with respect to other threads:

```java
public synchronized void withdraw(int amount, String who) { ... }
```

Why it works: `synchronized` enforces mutual exclusion on the BankAccount instance, eliminating concurrent execution of the critical section and removing the race.
- Trade-offs: Simple and effective for a single account. May cause contention and reduced throughput when scaled to many accounts or high rates of transactions.

Proposed Fix #2 — Atomic, lock-free update using `AtomicInteger`:
- Replace `int balance` with `AtomicInteger` and perform a CAS loop:

```java
int oldVal = balance.get();
if (oldVal < amount) return; // insufficient
int newVal = oldVal - amount;
if (balance.compareAndSet(oldVal, newVal)) { /* success */ }
else { retry }
```

Why it works: `compareAndSet` performs the conditional update atomically without blocking threads, providing better scalability.
- Trade-offs: Slightly more complex; may spin under heavy contention but usually faster than coarse locking.

Reproducibility & How to run:
From PowerShell (Windows):

```powershell
# compile
javac "C:\CSC325\BankSimulation\BankSimulationOriginal.java"
javac "C:\CSC325\BankSimulation\BankSimulationSynchronized.java"

# run original
java -cp "C:\CSC325\BankSimulation" BankSimulationOriginal

# run synchronized
java -cp "C:\CSC325\BankSimulation" BankSimulationSynchronized
```

Repeat the original run multiple times to observe final balances. The synchronized version should produce consistent final balances per run.

Before / After (short summary)
- Before: Multiple threads observed the same old balance and wrote overlapping updates; final balance varied between runs.
- After (synchronized): Withdrawals were serialized at the account-level; final balance is consistent and correct across runs.

Next steps (optional)
- Implement the `AtomicInteger` version and benchmark throughput under increasing thread counts.
- Replace manual `Thread` creation with `ExecutorService` and submit tasks for cleaner thread lifecycle management.
- Remove or reduce `fakeWork()` which wastes CPU and amplifies timing but is not realistic.

---

Files included with this submission:
- `BankSimulationOriginal.java` — original unsynchronized example (for reproducible before runs)
- `BankSimulationSynchronized.java` — synchronized fix (Level 2)
- `before_run.txt`, `after_run.txt` — representative console logs
- `ai_reflection.md` — 150–250 word reflection on AI use
