# javaSemaphoreBank
A program to practice using semaphores in Java, simulating a bank's operations

In this simulation, 50 customers go to a bank with 3 tellers working there.
The tellers must all arrive to the bank, when it then opens.
Customers choose randomly to either request to withdrawal or deposit to the bank.
Customers get in line, and then select a teller is one is available (each teller can work with only 1 customer at a time).
Customers introduce themselves to the teller, and then the teller asks them for their request.
For a withdrawal, the teller must talk to their manager (only one teller at a time) before they enter the safe (only two tellers at a time)
For a deposit, the teller must enter the safe (only two tellers at a time)
After the transaction is complete, the teller will inform the customer, who will then leave.
