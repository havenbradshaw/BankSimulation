AI Reflection — BankSimulation Concurrency Diagnosis

I used AI tools to help identify likely concurrency pitfalls and to draft possible code-level fixes. The AI correctly identified the classic check-then-act race condition on a shared non-atomic `int` and suggested typical remedies such as adding `synchronized` to the critical section and using `AtomicInteger` with a CAS loop. These suggestions were technically valid and served as a strong starting point.

Where the AI was less helpful was in certain practical trade-offs: for example, it suggested removing or reworking the `fakeWork()` function without clarifying that the artificial busy loop primarily amplifies timing windows to make the race easier to observe — removing it would hide symptoms but not address the root cause. I therefore verified all AI proposals by running the program before and after changes.

I implemented the `synchronized` fix and re-ran the simulation to confirm deterministic behavior. I did not accept the AI's output blindly: I adapted the ideas to this codebase, tested them locally, and documented the results. The AI was a useful assistant for brainstorming fixes and explanations, but I validated and adapted its recommendations before applying them.

