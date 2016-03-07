## HotPatch
### 一、简介
HotPatch主要基于[安卓App热补丁动态修复框架](https://github.com/dodola/HotFix)以及[NuwaGradle](https://github.com/jasonross/NuwaGradle)开发，主要包括以下两个模块：</br>
1. 热修复库[project('patchlib')]：基于多dex加载原理实现class替换，达到热修复的目的（实际上需要下载补丁后下一次启动应用才能生效）；</br>
2. Gradle插件[project('plugin')]：通过比较当前版本与Tag版本之间的差异，自动生成补丁包；</br>
项目兼容混淆及非混淆模式，兼容com.android.tools.build:gradle所有版本；

### 二、使用
### 1. 生产模式
1) clone项目并导入AndroidStudio；</br>
2) 根据rootProject中build.gradle中注释发布自动补丁脚本project(':plugin')到本地；</br>
3) 产看project(':app')中注释（里面包含一些基本注意事项），了解patch相关配置；</br>
4) 将patch.makeTag设置为true，运行后会在app下patch目录生成mapping及hash文件，用于后续混淆配置以及补丁生成；</br>
5) 将patch.makeTag设置为false，运行后若代码有修改则会在"${project.buildDir}/outputs/patch/${variant}"生成补丁包"patch.apk"；</br>
6) 下发patch.apk到生产环境。

### 2. 自测模式
前四步同1所述，第五步的时候可以修改一些代码（如示例中的Bug类），然后：</br>
* 当使用com.android.tools.build:gradle：1.3.0及以下时，命令行运行 `gradle dexDebug`；</br>
* 当使用com.android.tools.build:gradle：1.4.+及以下时，命令行运行 `gradle transformClassesWithDexForDebug`；</br>
运行完成之后会在"${project.buildDir}/outputs/patch/${variant}"生成补丁包"patch.apk"，将该补丁包复制到手机sdcard下，重新运行应用便可看到修复效果。

### 三、参考项目
1. [安卓App热补丁动态修复框架](https://github.com/dodola/HotFix)
2. [NuwaGradle](https://github.com/jasonross/NuwaGradle)

### 四、参考文献
1. [Android动态加载技术 简单易懂的介绍方式](https://segmentfault.com/a/1190000004062866)
2. [安卓App热补丁动态修复技术介绍](http://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a&scene=0)
