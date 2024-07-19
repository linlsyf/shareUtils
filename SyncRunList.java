package common.Sync;

import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class SyncRunList<E> {
    /**所有存储监护锁*/
    private  ReentrantLock lock;
    /**元素数组*/
    private List<? extends E> ls;
    /**下一个待执行的索引 transient*/
    private transient int  takeIdx;

    protected Map<String, Object> args;

    public SyncRunList(List<? extends E> ls) {
       this(ls,null);
    }

    public SyncRunList(List<? extends E> ls, Map<String, Object> args) {
        this.lock=new ReentrantLock(false);
        this.ls = ls;
        this.args = args;
    }


    public E poll(){
        final   ReentrantLock  lock=this.lock;
        lock.lock();;
        try{
              if (takeIdx>=ls.size()){
                  return  null;
              }
              return ls.get(takeIdx++);

        }finally {
            lock.unlock();
        }

    }


    public  boolean  remove(E o){
        if (o==null){
            return  false;
        }
       final  List<? extends E> ls=this.ls;
        final   ReentrantLock  lock=this.lock;
        lock.lock();
        try{

            int idx=ls.indexOf(o);
              ls.remove(idx);
            if (idx<takeIdx){
               takeIdx--;
            }
            return true;

        }finally {
            lock.unlock();
        }

    }

    /**
     * 引发所有线程的逐渐停止
     */

    public void toStopRunnb(){
        final   ReentrantLock  lock=this.lock;
        lock.lock();
        try{

            takeIdx=ls.size();


        }finally {
            lock.unlock();
        }
    }
    public int getTakeIdx(){
        final   ReentrantLock  lock=this.lock;
        lock.lock();
        try{

         return takeIdx;


        }finally {
            lock.unlock();
        }
    }
    /**单个线程执行结束后处理*/
    protected   abstract  void afterRun(SyncRunnb runnb);
    /**单元素执行任务*/
    protected   abstract  void runItem(E ITEM);
    /**任务执行线程*/
    protected   class  SyncRunnb  implements Runnable{

        @Override
        public void run() {
            try{

               E  item=null;
               while ((item=poll())!=null){
                   runItem(item);
                   item=null;
               }


            }finally {
               afterRun(this);
            }
        }
    }
    /**新开启线程*/
    public SyncRunnb  newRunRunnb(){
        return  new SyncRunnb();
    }
    /**线程池环境下遍历元素 执行既定的批量任务*/
    public void executeTasks(TaskExecutor taskExecutor,int taskCount){

         for (int r=0;r<taskCount;) {
             try{

                 taskExecutor.execute(newRunRunnb());
                 r++;
             }catch (  TaskRejectedException e){
                 //任务不能接受
                 try{
                     Thread.sleep(1000L);
                 }catch (InterruptedException e1){

                 }
             }
         }

    }


    public List<? extends E> getLs(){
        return  ls;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
