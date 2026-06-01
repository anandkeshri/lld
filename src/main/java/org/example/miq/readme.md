Credit Card Discount

Question:
Implement a portion of a credit card payment system that takes the Bill amount, Credit card number as the input and returns the discounted bill amount if the credit card is valid, else an error if the credit card is invalid

Procedure to validate a credit card number
Luhn check or the Mod 10 check, which can be described as follows (for illustration,
consider the card number 4388576018402626):
Step 1. Double every second digit from right to left. If doubling of a digit results in a
two-digit number, add up the two digits to get a single-digit number (like for 12:1+2, 18=1+8).

Step 2. Now add all single-digit numbers from Step 1.
4 + 4 + 8 + 2 + 3 + 1 + 7 + 8 = 37

Step 3. Add all digits in the odd places from right to left in the card number.
6 + 6 + 0 + 8 + 0 + 7 + 8 + 3 = 38

Step 4. Sum the results from Step 2 and Step 3.
37 + 38 = 75

Step 5. If the result from Step 4 is divisible by 10, the card number is valid; otherwise, it is invalid.

Bill Discount Details
Discount is based on the starting digit of the credit card and the corresponding discount values can be seen below

4 for Visa cards - Discount 10%
5 for Master cards - Discount 15%
37 for American Express cards - Discount 5%
6 for Discover cards - No Discount

What you need to provide
We expect you to provide a working solution to the problem, at least for the scenarios provided; it may just consist of hard coded data in a set of unit tests. Please note that input/output or persistence are NOT required. Concentrate your efforts on coming up with a solution that is
Simple
Modular
Extensible
Testable



Examples
Input: {“Bill Amount”: 10000, “CC_Card”: “379354508162306”}
Output: {“CC_Status”:”Valid”, “Bill Amount”: 9050}

Input: {“Bill Amount”: 10000, “CC_Card”: “4388576018402626”}
Output: {“CC_Status”:”Invalid”, “Bill Amount”:10000}
