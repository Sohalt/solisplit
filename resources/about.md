# Solisplit

Solisplit is an application to share costs in a more solidaric way.

## How it works

1. You enter the total cost that you want to split.
2. You enter the names of everyone who wants to share the cost.
3. You get a link to a form where everyone can enter the maximum amount they would be willing to contribute to the shared expense. Share this link with all participants.
4. After everyone has entered their answer, you can check the result to see the actual share that everyone should pay:
  - if the sum of everyone's maximum contribution exceeds the total cost that you want to split, the total cost is divided among everyone, proportional to their answers. This means that everyone pays at most the amount they were comfortable with and the cost is split fairly, based on how much someone was willing to contribute.
  - if the sum of everyone's maximum contribution does not reach the total cost, the program cannot calculate a fair distribution and instead shows how much more money would be needed to reach the total cost. You can then start over from the beginning and hope that people will increase their maximum contribution or try reducing the total cost if your situation allows for it.

### Example

Let's say Alice, Bob, and Charlie live together in a shared appartment and want to buy a new fridge for 600€.

Alice offers to pay up to 250€, Bob offers 150€ and Charlie offers 150€.
Since the total of 250+150+150 = 550 is below the required 600, we cannot compute a fair distribution and instead show that they are 50€ short of reaching their total.

The three decide to do a second round and hope that everyone can increase their share a bit.

This time Alice offers to pay up to 400€, Bob and Charlie each offer to pay up to 200€. Since the total of 400+200+200 = 800 exceeds the required 600, we now split the 600 proportional to the offers. This results in the following shares:

- Alice: 600 * 400/800 = 300
- Bob: 600 * 200/800 = 150
- Charlie: 600 * 200/800 = 150

As we can see Bob and Charlie pay the same, since they entered the same maximum. Alice pays double what Bob or Charlie are paying, since her maximum was twice that of Bob or Charlie. Also everyone pays less than their stated maximum and the sum of everyone's share adds up to the required total.
