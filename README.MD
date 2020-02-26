# YggdrasilOfficialProxy

YggdrasilOfficialProxy 采用了代理服务器的方式建立Yggdrasil服务器和
official服务器的人同时登录的支持nya. 并且提供了2种加载方式nya!

# 下载使用
如果你能熟练使用命令行启动java程序的话, 或者你想把 YggdrasilOfficialProxy
当做Java库使用. 下载 `dist\YggdrasilOfficialProxy-core.jar` nya.

如果你是懒得打命令, 匹配代码的话. 下载 `dist\YggdrasilOfficialProxy.jar` nya.
这个版本还包含了YOP使用的库nya.

# 使用的库

- [Netty](http://netty.io) - HTTP Server.
- [Gson](https://github.com/google/gson) - JSON parsing.
- [SnakeYaml](http://www.snakeyaml.org/) - YAML Parsing
- [JetBrains Annotations](https://www.jetbrains.com/help/idea/annotating-source-code.html) - Source Annotations

# 使用
## JavaAgent启动
你只需要把服务器的启动参数的
`-javagent:injector.jar=...` 替换成 `-javaagent:YggdrasilOfficialProxy.jar` 就好

```shell script
java -javaagent:YggdrasilOfficialProxy.jar -jar spigot-1.15.2.jar nogui
```

## 独立服务器启动
启动命令
```shell script
java -jar YggdrasilOfficialProxy.jar
```

# 配置
YggdrasilOfficialProxy 需要配置好才能使用.
我们的配置文件专门存在一个文件里面, 并不是从opt里面读取.
下面是我们的配置文件, 默认他叫做 `YggdrasilOfficialProxy.yml`.

如果有需要的话你还可以自定义配置路径

```shell script
java ... -javaagent:YggdrasilOffcialProxy.jar={path} ...
java -jar YggdrasilOffcialProxy.jar {path}
```
下面是我们的配置
```yaml
# Yggdrasil API 根地址
# 对于某 "统一"通行证,
# 他的地址是 https://auth2.nide8.com:233/{id}/
yggdrasil: "https://yggdrasil.example.com/"
# 检测玩家登录状态的时候先访问官方源
official-first: false
# 代理服务器端口
# 填 0 以自动分配
# 填 50000~51000
# 自动从 50000~51000中选择一个端口
port: 4321

# 代理读取超时时间(毫秒)
timed-out:
  reading: 60000
  connecting: 60000
# 链接代理
proxy:
  official:
    # direct 直接链接
    # socks  Socks代理
    # http   HTTP 代理
    type: "direct"
    host: "localhost"
    port: 1080
  yggdrasil:
    type: "direct"
extension:
  # 是否为MySkin之类的插件提供缺省地址修复
  yggdrasil-fix: true
  opts:
    injector:
      # 请替换成你自己的injector所在位置
      path: "G:/Java/authlib-injector-1.1.26-41a7a47.jar"
      # 没有需要的话不用改
      arg: "{proxy}"
```
