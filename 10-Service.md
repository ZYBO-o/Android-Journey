### 1.服务概念简介

+  **服务（service）** 是一种可在后台执行长时间运行操作而不提供界面的应用组件。服务可由其他应用组件启动，而且即使用户切换到其他应用，服务仍将在后台继续运行。此外，组件可通过绑定到服务与之进行交互，甚至是执行进程间通信 (IPC)。

+ 值得注意的是，服务不是运行在一个独立的进程当中，而是依赖于创建服务时所在的应用程序进程。当某个应用程序进程被杀掉时，所有依赖该进程的服务也会停止运行。
+ 而且实际上服务并不会自动开启现场，所有的代码都是默认运行在主线程当中的。也就是说需要再服务的内部手动创建子线程，并在这里执行具体的任务，否则有可能出现主线程被阻塞住的情况。

---

### 2.服务的基本用法

+  `startService()`方法

调用此方法启动服务，里面的参数可以是 `Intent` 对象。

+  `stopService()`方法

调用此方法停止服务，里面的参数可以是 `Intent` 对象。

> 上述两个方法都是定义在Context类中，在活动中可以直接调用这两个方法。

+  `stopSelf()`方法

可以在任意位置调用此方法使得服务自己停止。

---

+  `bindService()` 方法

可以调用此方法将服务与活动相绑定，其中接收三个参数：第一个参数为 `Intent` 对象；第二个参数为类对象，可以是类本身，或者匿名类等；第三个参数为标志位，如 `BIND_AUTO_CREATE` ，表示在活动和服务进行绑定后自动创建服务。

这会使得Service中 `onCreate()` 方法得到执行，但 `onStartCommand()` 不会执行。

+ `unbindService()` 方法

可以调用此方法解除活动与服务之间的绑定。

### 3.服务的生命周期

#### (1).基本介绍

服务基本上包含两种状态 ：

| 状态    | 描述                                                         |
| :------ | :----------------------------------------------------------- |
| Started | Android的应用程序组件，如活动，通过startService()启动了服务，则服务是Started状态。一旦启动，服务可以在后台无限期运行，即使启动它的组件已经被销毁。 |
| Bound   | 当Android的应用程序组件通过bindService()绑定了服务，则服务是Bound状态。Bound状态的服务提供了一个客户服务器接口来允许组件与服务进行交互，如发送请求，获取结果，甚至通过IPC来进行跨进程通信。 |

服务拥有生命周期方法，可以实现监控服务状态的变化，可以在合适的阶段执行工作。下面左图展示了当服务通过 `startService()` 被创建时的生命周期，右图则显示了当服务通过 `bindService()` 被创建时的生命周期：

<img src="images/3-1.png" width="50%;" />

+  `onCreate()`方法

在服务创建的时候调用。

+  `onStartCommand()`方法

在每次服务启动的时候调用。希望 服务启动时执行某个动作，则将逻辑写入其中。

+ `onBind()` 方法

当其他组件想要通过 `bindService()` 来绑定服务时，系统调用该方法。调用方可以获得到 `onBind()` 方法里返回的 `IBinder` 对象的实例，与服务进行通信。

+  `onDestroy()`方法

在服务销毁的时候调用，可以在其中回收那些不再使用的资源。

#### (2).启动方式及其区别

+ startService启动方式： `onCreate()--->onStartCommand() --->onDestroy()；`

+ bindService启动方式： `onCreate()--->onBind() --->onUnbind()--->onDestroy()；`

##### startService：

```java
Intent mIntent=new Intent(MainActivity.this,startServiceDemo.class) ;
 
startService(mIntent);//直接启动服务方式启动
 
stopService(mIntent);//停止服务
```

##### bindService：

```java
ServiceConnection serviceConnection=new myServiceConnect();
 
Intent mBindIntent=new Intent(MainActivity.this,bindServiceDemo.class);
 
bindService(mBindIntent,serviceConnection,BIND_AUTO_CREATE);//绑定服务
 
unbindService(serviceConnection);//解绑
```

 实现ServiceConnection接口

```java
 public class myServiceConnect implements ServiceConnection {
     //绑定成功之后回调，Service中传递的数据，在此中接收
     @Override
     public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
				//绑定时的逻辑
     }
     
     @Override
     public void onServiceDisconnected(ComponentName componentName) {  }
 }
```

##### 启动方式的区别

+ 重复调用是调用方法不同：
  + startService启动方式：重复调用时， `onCreate()` 方法只会创建调用一次， `startCommand()` 会每次都会被调用。
  + bindService启动方式：重复调用时， `onCreate()` 与 `onBind()` 都只会调用一次。
+ 与activity之间关系不同：
  + startService启动方式：与activity之间没有什么关系，对应的activity被销毁时，不影响service
  + bindService启动方式：与activity绑定，当对应的activity销毁时，对应的服务也销毁
+ 应用场景不同：
  + startService启动方式：与activity没有什么关系，所以常用于，只指定Service的操作，不需要service返回操作结果，不需要与Service建立联系的场景
  + bindService启动方式：activity需要Service操作完成后，返回一系列的返回结果的场景（这种场景也可以通过广播实现，但是操作频繁时会造成性能上的消耗）

---

### 4.使用IntentService

从前面可知，服务中的代码都是默认运行在主线程中，若直接在服务中处理一些耗时的逻辑，则容易出现ANR(Application Not Responding) 的情况。

面对上述情况，应使用多线程技术，在服务的每个具体方法里开启一个子线程，然后再去处理那些耗时操作。因此，一个比较标准的服务则可以写成：

```java 
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    new Thread(new Runable()){
        @Override
        public void run() {
						//处理的具体逻辑
          	stopSelf()//执行完成后自动停止
        }
    }
    return super.onStartCommand(intent, flags, startId);
}
```

为了防止一些细节问题，Android提供了一个 `IntentService` 类。

```java
public class MyIntentService extends IntentService {

    public MyIntentService() {
        super("MyIntentService"); // 调用父类的有参构造函数
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // 这个方法在子线程中执行，可以在里面除了一些具体的逻辑
        Log.d("MyIntentService", "Thread id is " + Thread.currentThread(). getId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
      	//这个服务在运行结束后是自动停止的。
        Log.d("MyIntentService", "onDestroy executed");
    }
}
```

































