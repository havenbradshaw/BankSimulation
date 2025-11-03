// BankSimulationOriginal.java
// Original unsynchronized variant (for reproducible 'before' runs)

import java.util.ArrayList;
import java.util.List;

public class BankSimulationOriginal {

    static class BankAccount {
        private int balance;

        public BankAccount(int startingBalance) {
            this.balance = startingBalance;
        }

        // Intentionally UNSAFE.
        public void withdraw(int amount, String who) {
            fakeWork();

            if (balance >= amount) {
                int oldBalance = balance;
                fakeWork();
                int newBalance = oldBalance - amount;
                balance = newBalance;

                System.out.println(
                    who + " withdrew $" + amount +
                    " | old balance = " + oldBalance +
                    " -> new balance = " + newBalance
                );
            } else {
                System.out.println(
                    who + " tried to withdraw $" + amount +
                    " but INSUFFICIENT FUNDS. (current balance = " + balance + ")"
                );
            }
        }

        public int getBalance() { return balance; }

        private void fakeWork() {
            for (int i = 0; i < 10; i++) {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    static class WithdrawTask implements Runnable {
        private final BankAccount account;
        private final String userName;
        private final int amountPerWithdrawal;
        private final int times;

        public WithdrawTask(BankAccount account, String userName, int amountPerWithdrawal, int times) {
            this.account = account; this.userName = userName; this.amountPerWithdrawal = amountPerWithdrawal; this.times = times;
        }

        @Override
        public void run() {
            for (int i = 0; i < times; i++) {
                account.withdraw(amountPerWithdrawal, userName);
                try { Thread.sleep(1); } catch (InterruptedException e) { }
            }
        }
    }

    public static void main(String[] args) {
        BankAccount shared = new BankAccount(1000);
        List<Thread> threads = new ArrayList<>();

        threads.add(new Thread(new WithdrawTask(shared, "Alice", 50, 10)));
        threads.add(new Thread(new WithdrawTask(shared, "Bob", 50, 10)));
        threads.add(new Thread(new WithdrawTask(shared, "Charlie", 50, 10)));
        threads.add(new Thread(new WithdrawTask(shared, "Diana", 50, 10)));
        threads.add(new Thread(new WithdrawTask(shared, "ATM-Kiosk", 20, 40)));

        System.out.println("=== Starting transactions with balance = $" + shared.getBalance() + " ===");
        long start = System.currentTimeMillis();

        for (Thread t : threads) t.start();
        for (Thread t : threads) { try { t.join(); } catch (InterruptedException e) { } }

        long end = System.currentTimeMillis();
        System.out.println("\n=== All transactions finished. ===");
        System.out.println("Actual FINAL balance reported by program = $" + shared.getBalance());
        System.out.println("Total runtime ms: " + (end - start));
    }
}
