import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Process {
    final int TIME_QUANTUMN = 2;

    //cpu time --
    //total io++ , current_io --
    //
    String status= "unstarted";
    int arrival_time;
    double penaltyR;
    int run_time;
    int current_cpu, total_cpu,  remain_current_cpu, remain_total_cpu, remain_before_current_cpu;      // =, =,-, -
    int current_io, remain_current_io,remain_before_current_io;         // = , -
    int total_io_time, total_finish_time, turnaround_time , total_wait_time;    //summary data ++
    int A, B,C, M, order;
    int quantumnLeft;




    //constructor
    Process(int A, int B, int C, int M, int order ){
        arrival_time = A;
        total_cpu = C;
        penaltyR =0; run_time= 0;

        //local with respect to  each stage process is at - ready run block
        remain_total_cpu = total_cpu;
        current_cpu = 0;    //io calculate
        remain_current_cpu = 0;     //determine if run to block --
        current_io=0;
        remain_current_io=0;
        remain_before_current_cpu=0;
        remain_before_current_io = 0;
        quantumnLeft = TIME_QUANTUMN;   //2

        //summary data
        total_finish_time=A;  //total
        turnaround_time = 0;
        total_io_time=0;
        total_wait_time=0;    //total
        this.A = A; this.B = B; this.C = C; this.M = M; this.order = order;
    }

    public StringBuilder PrintABCM(){
        StringBuilder sb = new StringBuilder();
        sb.append( "("+ this.arrival_time+" "+this.B+" "+ total_cpu+" "+ M+")");
        return sb;
    }   //for
    public StringBuilder PrintABCMComa(){
        StringBuilder sb = new StringBuilder();
        sb.append( "("+ arrival_time+", "+ B+", "+ total_cpu +", "+ M+")");
        return sb;
    }

    //ready
    public void setReady(){ keepReady(); }
    public void keepReady(){
        this.status = "ready";  //waiting
        this.total_wait_time++;
        total_finish_time++;
    }


    //if cpu burst< remaining cpu
    //cpu burst generate
    public void setRun(ArrayList<Integer> randomList){
        //normal set Run
        if(this.remain_current_cpu==0) {
            this.current_cpu = randomOS(randomList.remove(0), B); //generate current cpu burst
            if (this.current_cpu > remain_total_cpu) {
                this.current_cpu = this.remain_total_cpu; //update if needed
            }
            this.remain_current_cpu = this.current_cpu; //remain used to subtract
            quantumnLeft = TIME_QUANTUMN;
            if (this.quantumnLeft > remain_current_cpu) {
                this.quantumnLeft = this.remain_current_cpu; //update if needed
            }

            keepRun();
        }

        // after preempted re set Run
        else{
            this.remain_current_cpu = remain_current_cpu;
            quantumnLeft = TIME_QUANTUMN;

            //need this?
            if (this.quantumnLeft > remain_current_cpu) {
                this.quantumnLeft = this.remain_current_cpu; //update if needed
            }

            keepRun();

        }



    }
    public void keepRun(){
        quantumnLeft--;
        this.status = "running";
        this.remain_total_cpu--; //remain total cpu time
        this.remain_before_current_cpu = remain_current_cpu;        //for printing
        this.remain_current_cpu--;
        total_finish_time++;
        this.run_time++;
    }





    public static int randomOS(int X, int U) {
        return 1 + (X % U);
    }



    //generate io time
    public void setBlocked(){
        this.status = "blocked";
        current_io = current_cpu*M; //io generate
        remain_current_io = current_io;
        keepBlocked();
    }
    public void keepBlocked(){
        this.status = "blocked";
        this.remain_before_current_io = remain_current_io;
        this.remain_current_io--;
        this.total_io_time++;
        total_finish_time++;

    }
    public boolean NoIO(){return (M*current_cpu==0);}

    //first time done
    public void setDone(){
        this.status = "terminate";
        //total_finish_time++;
        turnaround_time = total_finish_time - arrival_time;   //TAT
    }
    public void keepDone(){
        this.status = "done";
    }

//    public void setPreempted(){
//        this.status = "preempted";
//        this.total_wait_time++; //goes into ready
//        total_finish_time++;
//    }





    //status
    public boolean isUnstarted(){return this.status.equals("unstarted");}
    public boolean isRun(){ return this.status.equals("running");}
    public boolean isReady(){ return this.status.equals("ready");}
    public boolean isBlocked(){return this.status.equals("blocked"); }
    public boolean isDone(){ return this.status.equals("terminate"); }
//    public boolean isPreempted(){ return this.status.equals("preempted"); }

    public boolean shouldPreempt(){
        return (quantumnLeft==0);
    }






}
