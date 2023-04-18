import java.io.*;
import java.util.*;

public class MainHPRN {

    static ArrayList<Process> processUnsorted;
    static ArrayList<Process> processSorted;
    static ArrayList<Integer> randomList;

    //storing position
    static PriorityQueue<Process> readyq = new PriorityQueue<>(new Comparator<Process>() {
        @Override
        public int compare(Process p1, Process p2) {
            if(p1.penaltyR < p2.penaltyR)
                return 1;
            else if(p1.penaltyR > p2.penaltyR)
                return -1;
            else {
                if (processSorted.indexOf(p1) < processSorted.indexOf(p2))
                    return -1;
                else if (processSorted.indexOf(p1) > processSorted.indexOf(p2))
                    return 1;
                else
                    return 0;
            }
        }
    });

    //time
    static int T = 0; //global time
    static boolean runningOccupied = false;
    static Process runningP;    //running process


    //representation
    static boolean detailed = false;
    static boolean show_ramdom = false;


    //io
    static File file;
    static String algo ="none";
    static int NumProcess;
    static int allDone; //0 - done

    //print
    static String rawdata;      //first 2 lines
    static double FINAL_FINISH_TIME;
    static double Final_RUN_TIME;
    static double Final_IO_TIME;
    static Boolean someProcessBlockedAtT;
    static double CPU_UTILIZATION;
    static double IO_UTILIZATION;
    static double Throughput;
    static double TOTAL_TURNAROUND, AVERAGE_TURNAROUND;
    static double TOTAL_WAITING_TIME, AVERAGE_WAITING_TIME;

    static StringBuilder res;   //print detail result from different algo






    public static void main(String[] args) throws IOException {
        readInFile(args); //everytime, re-read file - new Process
        readRandomFile();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        res =HPRN();

        //printing result
        FinalPrint();

    }


    //check detailed/random
    //sort data
    public static void readInFile(String[] args) throws IOException {

        //determine presentation style
        if (args.length == 2 && args[1].equals("--verbose")){
            detailed = true;
            //System.out.println("This detailed printout gives the state and remaining burst for each process");
        }
        else if(args.length==3 && args[2].equals("--random")) {
            show_ramdom = true;
        }

        //read in input
       file = new File(args[0]);
       BufferedReader reader = new BufferedReader(new FileReader(file));
        // BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        rawdata = reader.readLine();
        String[] input = rawdata.split("[()]" );

        NumProcess = Integer.parseInt(input[0].substring(0,1));
        allDone = NumProcess;
        processUnsorted = new ArrayList<>();

        //read in file
        int order = 0;
        for(String s: input){
            //System.out.println(s);
            String[] ABCM = s.split(" ");
            if(ABCM.length==4){                     // this condition skips the first ABCM first NumProcess
                int A = Integer.parseInt(ABCM[0]);
                int B = Integer.parseInt(ABCM[1]);
                int C = Integer.parseInt(ABCM[2]);
                int M = Integer.parseInt(ABCM[3]);
                Process process = new Process(A,B,C,M,order);
                processUnsorted.add(process);
                order++;
            }
        }

        //sort
        sortInput();

    }
    public static void sortInput(){
        processSorted = new ArrayList<Process>();
        HashMap<Integer, ArrayList<Process> > map = new HashMap<>();    //map each arrival time to process
        TreeSet<Integer> sortedArrive = new TreeSet<>();        //sort arrival time

        for(int i =0; i< processUnsorted.size();i++){
            Process process = processUnsorted.get(i);
            int arrive = process.arrival_time;  //arrive time
            sortedArrive.add(arrive);   //sort time by tree set

            if(map.containsKey(arrive)){ //map create
                map.get(arrive).add(process);   //arrive exist
            }else{
                ArrayList<Process> arrl = new ArrayList<>();
                arrl.add(process);  // add new arrival time
                map.put(arrive, arrl);
            }
        }

        //iterate through sorted arrive time, find its value pair in may - arraylist of processes
        while(!sortedArrive.isEmpty()) {
            int t = sortedArrive.pollFirst();
            ArrayList<Process> processesAtT = map.get(t);
            for (int k = 0; k < processesAtT.size(); k++) {
                processSorted.add(processesAtT.get(k));
            }
        }


//        //check sort
//        System.out.println("\nsorted:");
//        for(int i =0; i< processSorted.size();i++){
//            System.out.println(processSorted.get(i).arrival_time + " "+ processSorted.get(i).order);
//        }

    }





    //random File
    public static void readRandomFile() throws IOException {
        //          /Users/sylsyl/IdeaProjects/os_lab2/src/random-numbers     random-numbers
        Scanner sc = new Scanner(new File("random-numbers"));
        randomList = new ArrayList<>();
        while(sc.hasNextLine()) {
            int X = Integer.parseInt(sc.nextLine());
            randomList.add(X);
        }
    }





    //update run to process
    //update static too
    public static void updateRun( Process p, boolean set){
        if(set){
            p.setRun(randomList);   //set run
        }
        else{
            p.keepRun();            //keep run
        }
        runningP = p;// pop out - run
        runningOccupied = true;
        Final_RUN_TIME++;
    }

    //set keep the same
    public static void updateReady(Process p, boolean set){
        double t = Math.max(1, p.run_time);
        double timeInSystem = p.total_finish_time-p.arrival_time;
        p.penaltyR = timeInSystem / t;

        readyq.add(p);  //set ready
        if(set){
            p.setReady();
        }else{
            p.keepReady();
        }
        TOTAL_WAITING_TIME++;
    }

    public static void updateBlocked( Process p, boolean set){
        someProcessBlockedAtT = true;
        if(set){
            p.setBlocked();
        }else{
            p.keepBlocked();
        }
    }
    public static void updateDone( Process p, boolean set){
        if(set){
            allDone--;  //not everyone is done
            p.setDone();
            TOTAL_TURNAROUND += p.turnaround_time;
        }else{
            p.keepDone();
        }

    }


    public static StringBuilder HPRN(){
        StringBuilder sb = new StringBuilder();

        while(allDone!=0) {

            sb.append(String.format("Before cycle %3d:\t", T));
            //check IO_UTILIZATION
            someProcessBlockedAtT = false;

            int runningIndex =-1;
            int remain_before_current_cpu = -1;
            //update the running Process first
            if(runningP!=null){
                remain_before_current_cpu = runningP.remain_before_current_cpu;
                runningIndex = processSorted.indexOf(runningP);
                if(runningP.remain_current_cpu==0){
                    //check total cpu
                    if(runningP.remain_total_cpu==0)
                        updateDone(runningP, true);  //set done
                    else{
                        if(runningP.NoIO() ) {
                            updateReady(runningP, true); //set ready bc no IO burst for this process
                        }
                        else{
                            updateBlocked(runningP, true);   //set block
                        }
                    }

                    //update runningOccupied and runningP to be false, so the next process at the same T can be running
                    if(runningP.remain_current_cpu == 0 && runningP.remain_before_current_cpu==1) {
                        runningOccupied = false;
                        runningP = null;
                    }

                }else
                    updateRun(runningP, false);//keep run
            }



            for(int i =0; i<processSorted.size();i++){

                if(i==runningIndex){
                    sb.append(String.format("%12s %s.", "running", remain_before_current_cpu));
                    continue;
                }

                Process current = processSorted.get(i);

                if( current.isReady()|| current.isDone() || current.isUnstarted())
                    sb.append(String.format("%12s %s.", current.status, 0));
                else if( current.isRun())
                    sb.append(String.format("%12s %s.", current.status, current.remain_before_current_cpu));
                else if(current.isBlocked())
                    sb.append(String.format("%12s %s.", current.status, current.remain_before_current_io));

                //unstarted
                if (current.isUnstarted()) {
                    if(T==current.arrival_time){
                        updateReady(current, true); //set ready
                    }
                }

                //ready
                else if(current.isReady()){
                        updateReady(current,false); //keep ready
                }


                //running - done, ready, block. keep run
                else if (current.isRun()){
                    //current cpu run out
                    if(current.remain_current_cpu==0){
                        //check total cpu
                        if(current.remain_total_cpu==0)
                            updateDone(current, true);  //set done
                        else{
                            if(current.NoIO() ) {
                                updateReady(current, true); //set ready bc no IO burst for this process
                            }
                            else{
                                updateBlocked(current, true);   //set block
                            }
                        }

                        //update runningOccupied and runningP to be false, so the next process at the same T can be running
                        if(current.remain_current_cpu == 0 && current.remain_before_current_cpu==1) {
                            runningOccupied = false;
                            runningP = null;
                        }

                    }else
                        updateRun(current, false);//keep run
                }



                //blocked
                else if (current.isBlocked()){
                    //no io decide if goes to ready/run
                    if(current.remain_current_io==0) {
                            updateReady(current,true); //set run
                    }
                    else {
                        updateBlocked(current, false);  //keep block
                    }
                }


                //done
                else if(current.isDone())
                    continue;       //no need to update, update increase finish time

            }

           if ( !readyq.isEmpty() && !runningOccupied){
               Process p = readyq.poll();
               updateRun(p, true);      //find the max penalty, update to Run
               p.total_wait_time--;             //so it wasn't at ready at all
               p.total_finish_time--;
               TOTAL_WAITING_TIME--;
           }

           T++;
           sb.append("\n");
           if(someProcessBlockedAtT) Final_IO_TIME++;

           readyq = new PriorityQueue<>(new Comparator<Process>() {
                @Override
                public int compare(Process p1, Process p2) {
                    if(p1.penaltyR < p2.penaltyR)
                        return 1;
                    else if(p1.penaltyR > p2.penaltyR)
                        return -1;
                    else{
                        if( processSorted.indexOf(p1) < processSorted.indexOf(p2)  )
                            return -1;
                        else if( processSorted.indexOf(p1) > processSorted.indexOf(p2)  )
                            return 1;
                        else
                            return 0;
                    }
                }
            });
        }

        FINAL_FINISH_TIME = T-1;
        return sb;

    }

    public static StringBuilder RR(){
        StringBuilder sb = new StringBuilder();
        return sb;
    }




    //decide detailed mode
    // 3 parts: first two lines, detailed , summary
    public static StringBuilder FinalPrint(){
        StringBuilder sb = new StringBuilder("\n");

        //first two lines
        sb.append("The original input was: "+NumProcess+" ");   //unsorted
        for(Process p: processUnsorted){
            sb.append(p.PrintABCM()+" ");
        }
        sb.append("\n");
        sb.append("The (sorted) input is:  "+NumProcess+" "); //sorted
        for(Process p: processSorted){
            sb.append(p.PrintABCM()+" ");
        }
        sb.append("\n\n");      //space one line afterwards


        //check detailed
       if(detailed)
            sb.append(PrintDetail()+"\n\n");

        //print Process Data
        sb.append(PrintProcessData()+"\n\n");

        //print summary
        sb.append(PrintSummary()+"\n\n");

        System.out.println(sb);
        return sb;
    }

    //used by FINALPRINT
    public static StringBuilder PrintDetail(){
        StringBuilder sb = new StringBuilder("");
        sb.append("This detailed printout gives the state and remaining burst for each process\n\n");
        sb.append(res);
        return sb;
    }
    public static StringBuilder PrintProcessData(){
        StringBuilder sb = new StringBuilder("");


        sb.append("The scheduling algorithm used was Highest Penalty Ratio Next\n");

        //each process
        for(int i=0; i<processSorted.size(); i++){
            Process p = processSorted.get(i);
            sb.append("Process "+i+":\n");
            sb.append("\t (A,B,C,M) = "+ p.PrintABCMComa()+"\n");
            sb.append("\t Finishing time: "+ p.total_finish_time+ "\n");
            sb.append("\t Turnaround time: "+ p.turnaround_time+"\n");
            sb.append("\t IO time: "+p.total_io_time+"\n");
            sb.append("\t Waiting time: "+p.total_wait_time+"\n");
        }

        return sb;
    }
    public static StringBuilder PrintSummary(){

        CPU_UTILIZATION = Final_RUN_TIME/ FINAL_FINISH_TIME ;       //only 1 process can run at the same time
        IO_UTILIZATION = Final_IO_TIME/ FINAL_FINISH_TIME;          //multiple can be blocked at the same time
        Throughput = NumProcess/FINAL_FINISH_TIME*100;
        AVERAGE_TURNAROUND = TOTAL_TURNAROUND/NumProcess;
        AVERAGE_WAITING_TIME = TOTAL_WAITING_TIME/NumProcess;

        StringBuilder sb = new StringBuilder("Summary Data:\n");
        sb.append("\t Finishing time: "+ (int)FINAL_FINISH_TIME + "\n");
        sb.append("\t CPU Utilization:"+ String.format( "%.6f", CPU_UTILIZATION) +"\n");
        sb.append("\t IO Utilization:"+String.format( "%.6f", IO_UTILIZATION) +" \n");
        sb.append("\t Throughput: "+ String.format( "%.6f", Throughput)+" process per hundred cycles\n");          //"+ +"
        sb.append("\t Average turnaround time:"+String.format( "%.6f", AVERAGE_TURNAROUND)+ "\n");
        sb.append("\t Average waiting time: "+String.format( "%.6f", AVERAGE_WAITING_TIME)+ " \n");

        return sb;
    }       //not finished






}
