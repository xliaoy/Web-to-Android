# 株馨HTML安卓应用开发模板

一个由Java开发的HTML打包安卓APP的演示项目

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 8 |
| 最低 SDK | API 24 (Android 7.0) |
| 编译 SDK | 34 |
| 构建工具 | AGP 8.2.0 / Gradle 8.5 |


## 调用说明

必须先用js引入下方的Bridge
```
    // ===== Bridge必须先引入 =====
    window.__bridgeReady = new Promise(function(resolve) {
      if (typeof AndroidBridge !== 'undefined') { resolve(); return; }
      window.onBridgeReady = resolve;
    });
```

## 添加 zxui 接口：

在 `MainActivity.java` 里的 `WebAppBridge` 内部类加 `@JavascriptInterface` 方法即可。当前是四个：

```
MainActivity.java  ~第300行
├── WebAppBridge 内部类
│   ├── 获取外部存储目录()   → 返回 /storage/emulated/0/
│   ├── 判断指定文件(path)   → 检查文件是否存在
│   ├── 保存指定文件(path, data) → 保存文件
│   └── 执行JAVA(code)      → 重启应用
```

需要新方法的按同样格式加进去，JS 端直接 `zxui.方法名(...)` 调用。

## 权限管理：

`PermissionManager.java`。当前自动处理存储权限（READ/WRITE/MANAGE_EXTERNAL_STORAGE），新增权限在 `isDangerousPermission()` 里加判断即可，框架自动扫描 Manifest 并在 `onResume` 申请。



## 赞赏一下 

如果这个工具对您有帮助，欢迎赞赏支持，您的支持是我持续维护的动力！🎉

| 微信支付 | 支付宝 |
|:---:|:---:|
| ![微信支付](wx.png) | ![支付宝](zfb.png) |

---