# RaiixDMServer For Minecraft Fabric
![Java CI](https://github.com/rayxuln/RaiixDMServer/workflows/Java%20CI/badge.svg?branch=1.18.1)

## 特性
- 接收B站直播弹幕
- 自定义弹幕
- 服务器端
- 接收多房间

## 指令帮助
### dmsconnect 房间号

连接至指定房间

### dmsdisconnect 房间号/all

断开指定房间

### dmsreconnect 房间号/all

重新连接到房间，使用all将重连所有包括已断开的房间

### dmsinfo 房间号

显示房间的人气值等信息

### dmslist

列出所有房间信息，包括已断开连接的房间

### dmsset 房间号/default/all 键 值

设置房间内配置信息，配置信息参考配置文件或者下文

### dmsget 房间号/default/all 键

获取房间内配置信息，配置信息参考配置文件或者下文

### dmsreload

重新加载配置文件，需要op4权限

### dmsaddroom 房间号

添加房间，不连接

### dmsremoveroom 房间号/all

删除房间

## 自定义弹幕帮助

现以设置将某房间显示为：

`[来自Raiix的直播间的弹幕][Raiix_蘩_][UL13]<Raiix_蘩_>:这是一条示例弹幕啦~`

则使用指令将聊天弹幕样式(chat_dm_style的值)设为

`%GREEN%[来自{{owner}}的直播间的弹幕]%RED%[{{roomOwner}}]%GOLD%[UL{{uLevel}}]%WHITE%<{{danmuAuthur}}>:{{danmuMsg}}`

具体指令为：

`/dmsset all chat_dm_style "%GREEN%[来自{{owner}}的直播间的弹幕]%RED%[{{roomOwner}}]%GOLD%[UL{{uLevel}}]%WHITE%<{{danmuAuthur}}>:{{danmuMsg}}"`

其中的`owner`是自定义的键

接着输入指令设置指定房间的`owner`的值：

`/dmsset all owner "未定义"`

`/dmsset 274711 owner "Raiix"`

也就是说最后显示弹幕的时候，若是来自 **274711** 房间的弹幕，则会将`{{owner}}`替换成`Raiix`，
否则的话将会替换成`未定义`

### 可用于弹幕样式的合法的键
对于常规聊天弹幕
```
|键名         |   值     |
|----------  |---------  |
|uLevel      |用户等级   |
|danmuAuthur |弹幕发送者 |
|danmuMsg    |弹幕消息   |
|roomTitle   |房间标题   |
|roomOwner   |主播昵称   |
```

对于常规礼物弹幕
```
|键名         |   值     |
|----------  |---------  |
|danmuAuthur |弹幕发送者 |
|num         |礼物的数量 |
|actionName  |发礼物的动作 |
|giftName    |礼物名    |
|roomTitle   |房间标题  |
|roomOwner   |主播昵称  |
```

通过指令 `/dmsset <roomID> key value` 设置的自定义键也可用于弹幕样式

### 可使用`dmsset/dmsget`配置的键
```
|键名          |   值                                  |
|--------------|-------------------------------------  |
|black_dm      |黑名单关键词列表（使用'|'分割）          |
|white_dm      |白名单关键词列表（使用'|'分割）          |
|mode          |使用白名单或黑名单（填"black"或"white"） |
|platform      |弹幕平台，目前仅支持"bilibili"           |
|chat_dm_style |聊天弹幕样式                            |
|gift_dm_style |礼物弹幕样式                            |
|自定义键       |自己定义含义                            |
```
> 自定义键只能使用字母，数字和下划线哦~
> 而且样式字符串请勿输入包含'\\'的字符，否则可能引发崩溃!!!

## 许可证

本项目使用GPLv3许可证