## 一. 广播机制简介

Android 中的每个应用程序都可以对自己感兴趣的广播进行注册，这样该程序就只会接收到自己所关心的广播内容，这些广播可能来自与系统，也可能来自于其他应用程序。

Android 提供了一套完整的 API，允许应用程序自由地发送和接收广播。Intent 就是发送广播的方法之一，而接收广播的方法需要引入新的概念——广播接收器（Broadcast Receiver）。

Android 中的广播主要分为两种类型：

+  **标准广播（Normal broadcast）** ，进行完全异步执行，广播发出后，所有接收器几乎在同一时刻接收到，没有先后顺序可言。这种广播效率较高，但无法被截断。
+  **有序广播（Ordered broadcast）** ，进行同步执行，广播发出后，同一时刻只有一个接收器能收到消息，当接收器的逻辑执行完毕后，广播会继续传递。广播接收器按照优先级高低来先后接收消息，而且前面的广播接收器可以截断正在传递的广播。

## 二. 接收系统广播

Android 内置了很多系统级别的广播，可以在应用程序中通过监听这些广播来得到各种系统的状态信息。

广播接收器可以自由地对自己感兴趣的广播进行注册，这样当有相应的广播发出时，广播接收器就能够接收到该广播，并在内部处理对应的逻辑。注册广播的方式一般有两种：

+ 在代码中注册，即 **动态注册** 。
+ 在 `AndroidManifest.xml` 中注册，即 **静态注册** 。

### 1. 动态注册监听网络变化

>  通过动态注册的方式编写监听网络变化的程序。

系统中每当网络状态发生变化时，系统会发出一条值为 `android.net.conn.CONNECTIVITY_CHANGE` 的广播，所以可以在广播其中添加对应的 action 进行监听，并进行注册。

1. 在 `MainActivity` 中定义一个内部类 `NetworkChangeReceiver` ，这个类继承自 `BroadcastReceiver` ，并重写 `onReceive()` 方法。当网络状态发生变化时，  `onReceive()` 方法会得到执行。
2.  在 `onCreate()` 方法中创建 `IntentFilter` 实例，并添加值为 `android.net.conn.CONNECTIVITY_CHANGE` 的广播。
3. 随后创建一个 `NetworkChangeReceiver` 的实例，然后调用 `registerReceiver()` 方法进行注册，将 `NetworkChangeReceiver` 实例与  `IntentFilter` 实例都传出去，这样 `NetworkChangeReceiver` 就能收到所有值为  `android.net.conn.CONNECTIVITY_CHANGE` 的广播，实现监听网络变化的功能。
4. 动态注册的广播接收器需要取消注册，一般都是在 `onDestory()` 方法中通过调用 `unregisterReceiver` 方法实现。
5. 值得注意的是，访问系统的网络状态需要在 `AndroidManifest.xml` 中声明权限

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

实现代码如下：

```java 
public class MainActivity extends AppCompatActivity {

    private IntentFilter intentFilter;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter = new IntentFilter();
        //创建 IntentFilter 实例后添加了一个值为 android.net.conn.CONNECTIVITY_CHANGE 的action
        //每当网络状态发生变化时，系统会发出值为 android.net.conn.CONNECTIVITY_CHANGE 的广播
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReceiver = new NetworkChangeReceiver();
        //调用 registerReceiver() 方法进行注册
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //动态注册的广播接收器需要取消注册
        unregisterReceiver(networkChangeReceiver);
    }

    class NetworkChangeReceiver extends BroadcastReceiver {

        //每当网络状态发生变化时，onReceive()方法就会得到执行
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isAvailable()) {
                Toast.makeText(context, "network is available", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "network is unavailable", Toast.LENGTH_SHORT).show();
            }


        }
    }
}
```

### 2.静态注册实现开机启动

动态注册的广播接收器可以自由控制注册与注销，具有较大的灵活性，但必须在程序启动后才能接收到广播，因为注册逻辑是写在 `onCreate()` 方法中的。如果需要让程序在未启动的情况下接收到广播，可以使用静态注册来实现。

>  通过静态注册的方式接收一条开机广播。

1. 首先在包目录创建 `Broadcast Receiver` ，并将该广播接收器命名为 `BootCompleteReceiver` 。
2. 在 `onReceive()` 函数中添加 `Toast` 提示。

```java
public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Boot Complete", Toast.LENGTH_SHORT).show();
    }
}
```

3. 在 `AndroidManifest.xml` 中注册，添加 `<receiver>` 标签，并在 `<intent-filter>` 中添加相应的 action ；此外，也需要声明监听系统开机的权限。

```xml
<!-- 添加权限 -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

<!-- 进行注册 -->
<receiver
	android:name=".BootCompleteReceiver"
	android:enabled="true"
	android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

## 三. 发送自定义广播

### 1. 发送标准广播

1. 首先建一个广播接收器准备接收广播，新建为MyBroadcastReceiver：

```java
public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context,"Receive MyBroadcastReceiver!", Toast.LENGTH_SHORT).show();
    }
}
```

2. 在 AndroidManifest.xml中对这个广播接收器进行注册。这里让 `MyBroadcastReceiver` 接收 一条值为 `com.example.broadcasttest.MY_BROADCAST` 的广播，因此待会儿在发送广播的时候，我们就需要发出这样的一条广播。

```xml
<receiver
		android:name=".AnotherBroadcastReceiver"
    android:enabled="true"
    android:exported="true">
    <!-- 添加内容 -->  
     <intent-filter>
     	 	<action android:name="com.example.broadcasttest.MY_BROADCAST"/>
     </intent-filter>
     <!-- 添加结束 -->
</receiver>
```

3. 在布局文件中定义了一个按钮，用于作为发送广播的触发点。

```xml
<Button
        android:id="@+id/btn"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="send"
        tools:ignore="MissingConstraints" />
```

4. 在按钮的点击事件里面加入了发送自定义广播的逻辑。首先构建出了一 个 Intent 对象，并把要发送的广播的值传入，然后调用了 Context 的 `sendBroadcast()` 方法将广 播发送出去，这样所有监听 `com.example.broadcasttest.MY_BROADCAST` 这条广播的广播接收器就会收到消息。

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button=findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent("com.example.broadcasttest.MY_BROADCAST");
                sendBroadcast(intent);
            }
        });
    }
}
```

### 2.发送有序广播

广播是一种可以跨进程的通信方式，这一点从前面接收系统广播的时候就可以看出来 了。因此在我们应用程序内发出的广播，其他的应用程序应该也是可以收到的。

新创建项目后可以创建 BroadCast ，并进行注册，有序广播中广播接收器是有先后顺序的，而且前面的广播接收器还可以将广播截断，以阻止其继续传播。

通过 `android:priority` 属性给广播接收器设置了优先级，优先级比较高的广播接收器就可以先收到广播。

```xml
<!-- 优先级设成了 100 -->
<intent-filter android:priority="100">
```

发送有序广播只需要改动一行代码，即将 `sendBroadcast()` 方法改成 `sendOrderedBroadcast()` 方法。 `sendOrderedBroadcast()` 方法接收两个参数，第一个参数仍然是 `Intent` ，第二个参数是一个与权限相关的字符串，这里传入 `null` 就行了。

```java
 public void onClick(View v) {
                Intent intent=new Intent("com.example.broadcasttest.MY_BROADCAST");
                //发送一条有序广播
                sendOrderedBroadcast(intent,null);
}

```

如果在 `onReceive()` 方法中调用了 `abortBroadcast()` 方法，就表示将这条广播截断，后面的 广播接收器将无法再接收到这条广播。

---

## 四.使用本地广播

系统全局广播容易引发安全性问题，所以 Android 引入了一套本地广播机制，只能在应用程序的内部进行传播，而且广播接收器也只能接收来自本应用程序发出的广播，则安全性问题就不存在了。

本地广播是无法通过静态注册的方式来接受的，因为在发送本地广播时，程序肯定已经启动了，无需静态注册的功能。

本地广播的用法并不复杂，主要就是使用了一个 `LocalBroadcastManager` 来对广播进行管理，并提供了发送广播和注册广播接收器的方法。

```java
public class MainActivity extends AppCompatActivity {
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);//得到实例
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.example.broadcasttest.LOCAL_BROADCAST");
                localBroadcastManager.sendBroadcast(intent);//发送广播
                sendBroadcast(intent);
            }
        });
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.broadcasttest.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);//注册广播接收器
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localReceiver);
    }
		//接受广播
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "received local broacast", Toast.LENGTH_SHORT).show();
        }
    }
}
```

和前面的动态注册广播十分相似。

```xml
 <intent-filter>
		<!-- 进行注册 -->
		<action android:name="com.example.broadcasttest.LOCAL_BROADCAST"/>
</intent-filter>

```





































































