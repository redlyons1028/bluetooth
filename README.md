# 手机蓝牙通讯信号的捕捉及绘制折线图

> 开发要求

1.手机示波器由手机端软件，数据采集探头（传感器）组成。传感器通过蓝牙(或wifi)传送到手机的数据，经程序处理后，以波形的方式在屏幕上显示。

2.传感器用pc机或另一台手机模拟，手机端包括简单的UI设计，蓝牙(或wifi)通信接口设计，波形显示和存储，数据库设计

3.程序要求稳健（要有异常处理），界面美观，操作方便，规定使用 Android4.2开发。

> 使用技术

- android嵌入式开发（蓝牙通讯）

- simplewaveform插件（用于绘制折线图），在dependencies里添加依赖
```
compile 'com.maxproj.simplewaveform:app:1.0.0'
```

> 部分截图

<div>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_1.png" height="350px"/>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_2.png" height="350px"/>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_3.png" height="350px"/>
<br>
<br>
<p>服务端 and 客户端</p>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_4.jpg" height="350px"/>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_5.png" height="350px"/>
<br>
<br>
<p>运行</p>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_6.png" height="350px"/>
<img src="https://github.com/redlyons1028/picture/blob/master/bluetooth_chat_7.jpg" height="350px"/>
</div>
<br>

> 参考资料

- [清风微抚的个人博客](http://blog.csdn.net/max2005/article/details/50507727)
