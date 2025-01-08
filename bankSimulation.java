import java.util.concurrent.Semaphore;
import java.util.Random;

public class bankSimulation{
    public static final int NUM_TELLER = 3;
    public static final int NUM_CUSTOMER = 50;
    
//~~~~~ TELLER ~~~~~
    static public Semaphore tellerLock = new Semaphore(1);  //used to prevent multiple access of tellerCount
    static public int tellerCount = 0; //the current number of tellers
    static public Semaphore openBank = new Semaphore(0);    //the bank starts off closed
    static public Semaphore manager = new Semaphore(1);     //1 teller may speak to the manager at a time
    static public Semaphore safe = new Semaphore(2);        //2 tellers may be in the safe at a time

    //semaphores from customer to wake up teller
    static public Semaphore[] customertoTeller = {new Semaphore(0), new Semaphore(0), new Semaphore(0)};
    //semaphores from teller to wake up customer
    static public Semaphore[] tellerToCustomer = {new Semaphore(0), new Semaphore(0), new Semaphore(0)};
    //integers used for communication between customer and teller; communicate at the index of the teller
    static public int[] intCustomerTeller = {-1, -1, -1};


    static public int customersLeft = NUM_CUSTOMER; //the number of customers that will appear in the day
    
    static public class Teller extends Thread
    {
        int id; //store the id of the entity in the simulation

        Teller(int id)
        {
            this.id = id;
        }

        // code for tellers to run
        public void run()
        {
            try
            {
                tellerLock.acquire();    //first teller can continue, all else must block
                tellerCount++;
                System.out.println("Teller " + id + " has arrived, bringing the total to " + tellerCount + " tellers.");
                if (tellerCount == NUM_TELLER) openBank.release();    //if the final teller has arrived, they will open the bank.
                tellerLock.release();    //the next teller may arrive
                Random stall = new Random();

                int curCustomer = -1;
                while(customersLeft > 0)
                {
                    //wait until a customer approaches
                    customertoTeller[id].acquire();
                    curCustomer = intCustomerTeller[id];    //customer gave teller its id

                    //if there are no customers left, exit
                    if (customersLeft > 0)
                    {
                        System.out.println("Teller " + id + " is serving Customer " + curCustomer + ".");
                        tellerToCustomer[id].release(); //tell the customer to give a transaction type
                        
                        //wait for customer to tell what type of transaction
                        customertoTeller[id].acquire();
                        if (intCustomerTeller[id] == 1)
                        {
                            //deposit
                            System.out.println("Teller " + id + " is handling the deposit transaction from Customer " + curCustomer + ".");
                        }
                        else
                        {
                            //withdrawal; requires talking to manager
                            System.out.println("Teller " + id + " is handling the withdrawal transaction from Customer " + curCustomer + ".");
                            System.out.println("Teller " + id + " is going to the manager.");
                            //wait for manager to be open to get permission
                            manager.acquire();
                            //talk with mananger for a while
                            System.out.println("Teller " + id + " is getting the manager's permission.");
                            Thread.sleep(stall.nextInt(25) + 5); //sleep for 5 - 30 ms
                            System.out.println("Teller " + id + " got the manager's permission.");
                            manager.release();  //allow other tellers to talk to manager
                        }

                        System.out.println("Teller " + id + " is going to the safe.");
                        //wait for safe to have room
                        safe.acquire();
                        System.out.println("Teller " + id + " is in the safe.");
                        safe.release(); //free up room within the safe

                        //do transaction in safe for a while
                        System.out.println("Teller " + id + " is performing the transaction within the safe.");
                        Thread.sleep(stall.nextInt(40) + 10);   //sleep for 10 - 50 ms
                        System.out.println("Teller " + id + " has completed the transaction, and has informed Customer " + curCustomer + ".");

                        //end transaction
                        intCustomerTeller[id] = -1; //reset communication integer
                        tellerToCustomer[id].release(); //tell customer their transaction is finished


                    }
                }
            }
            catch(Exception e)
            {
                System.err.println("Error in Teller " + id + ": " + e);
            }
        }
    }
//~~~~~ END TELLER ~~~~~


//~~~~~ CUSTOMER ~~~~~
    static public Semaphore frontDoor = new Semaphore(2);           //only 2 customers can use the door at once
    static public Semaphore availableTellers = new Semaphore(3);    //number of tellers currently available
    static public Semaphore choosingTeller = new Semaphore(1);  //only 1 customer should be allowed to choose at a time for mut.ex.

    static public class Customer extends Thread
    {
        int id; //store the id of the entity in the simulation

        Customer(int id)
        {
            this.id = id;
        }

        //code for customers to run
        public void run()
        {
            try
            {
                Random rand = new Random(); 
                int randomTransaction = rand.nextInt(2);    //randomly decide either deposit or withdrawal
                if (randomTransaction == 1) System.out.println("Customer " + id + " wants to make a deposit.");
                else System.out.println("Customer " + id + " wants to make a withdrawal.");
                System.out.println("Customer " + id + " is going to the bank.");

                //attempt to enter the doorway, if there is room
                frontDoor.acquire();
                System.out.println("Customer " +  id + " is getting in line.");
                frontDoor.release();    //this customer has entered, so their space in the doorway is open
                
                //wait for a teller to open
                availableTellers.acquire();
                System.out.println("Customer " + id + " is selecting a teller.");

                //only one customer may choose a teller at a time for mut.ex.
                choosingTeller.acquire();
                int chosenTeller = CustomerChooseTeller(id);
                System.out.println("Customer " + id + " goes to Teller " + chosenTeller + ".");
                System.out.println("Customer " + id + " introduces itself to Teller " + chosenTeller + ".");
                customertoTeller[chosenTeller].release();   //wake up teller
                System.out.println("Customer " + id + " is waiting for Teller " + chosenTeller + " to ask for their transaction.");
                choosingTeller.release();   //the next customer may choose a teller

                //wait for teller to ask for type of transaction
                tellerToCustomer[chosenTeller].acquire();   
                intCustomerTeller[chosenTeller] = randomTransaction;   //randomly answer teller with either 0 or 1
                if (randomTransaction == 1)
                {
                    System.out.println("Customer " + id + " asks for a deposit transaction from Teller " + chosenTeller + ".");
                }
                else
                {
                    System.out.println("Customer " + id + " asks for a withdrawal transaction from Teller " + chosenTeller + ".");
                }
                customertoTeller[chosenTeller].release();   //inform teller of answer
                System.out.println("Customer " + id + " is waiting patiently on Teller " + chosenTeller + ".");

                //wait for response from teller
                tellerToCustomer[chosenTeller].acquire();
                System.out.println("Customer " + id + " is leaving the bank.");
                customersLeft--;
                availableTellers.release(); //done with teller, free up for the next customer
            }
            catch(Exception e)
            {
                System.err.println("Error in Customer " + id + ": " + e);
            }
        }
    }
    //function called by the customer to figure out which teller is the first available and to choose them
    static int CustomerChooseTeller(int id)
    {
        int chosenTeller = -1;
        if (intCustomerTeller[0] == -1)  
        {
            //0th teller is open
            chosenTeller = 0;
            intCustomerTeller[0] = id;
        }
        else if (intCustomerTeller[1] == -1)
        {
            //1st teller is open
            chosenTeller = 1;
            intCustomerTeller[1] = id;
        }
        else if (intCustomerTeller[2] == -1)
        {
            //2nd teller is open
            chosenTeller = 2;
            intCustomerTeller[2] = id;
        }
        return chosenTeller;
    }
//~~~~~ END CUSTOMER ~~~~~

    public static void main(String[] args) throws Exception
    {
        /*File file = new File("output.txt");
		FileOutputStream fos = new FileOutputStream(file);
		PrintStream ps = new PrintStream(fos);
        System.setOut(ps);*/

        System.out.println("The day has started.");

        //create tellers
        Teller[] tellers = new Teller[NUM_TELLER];
        for (int i = 0; i < NUM_TELLER; i++)
        {
            tellers[i] = new Teller(i);
            tellers[i].start();
        }
        //the bank will not open until all tellers arrive.
        openBank.acquire();
        System.out.println("The bank is open.");

        //create customers
        Customer[] customers = new Customer[NUM_CUSTOMER];
        for (int i = 0; i < NUM_CUSTOMER; i++)
        {
            customers[i] = new Customer(i);
            customers[i].start();
        }

        //wait for customers to exit
        for(int i = 0; i < NUM_CUSTOMER; i++)
        {
            try {
                customers[i].join();
            } catch (InterruptedException e) {
                System.err.println("Error joining with Customer " + i + ": " + e);
            }
        }

        //when all customers are gone, wake up all tellers
        for (int i = 0; i < NUM_TELLER; i++)
        {
            customertoTeller[i].release();
        }

        //wait for tellers to exit
        for(int i = 0; i < NUM_TELLER; i++)
        {
            try {
                tellers[i].join();
            } catch (InterruptedException e) {
                System.err.println("Error joining with Teller " + i + ": " + e);
            }
        }

        //simulation has ended
        System.out.println("The bank is closed.");
    }
}
