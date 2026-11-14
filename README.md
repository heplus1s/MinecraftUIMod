# 界面总库 / Minecraft UI packs

这里是这些 Fabric 1.20.1 模组的可选客户端界面的唯一存放处。服务器上的银行和领地规则仍在各自的仓库里：

- 银行核心：[MinecraftBankMod](https://github.com/heplus1s/MinecraftBankMod)
- 领地核心：[MinecraftLandMod](https://github.com/heplus1s/MinecraftLandMod)

这个仓库不包含服务器账本，也不包含领地保护和存档。装不装界面，都不影响能不能进服。

This repository is the only home for the optional client screens of these Fabric 1.20.1 mods. The bank ledger and the land protection stay in their own repositories, linked above. Nothing in this repository is required to join a server.

## 安装

服务器只需要核心 jar：

- `bankmod`，来自 MinecraftBankMod
- `landmod`，来自 MinecraftLandMod

还要有 Fabric Loader 和 Fabric API。玩家用原版客户端也能进服。不装下面的界面时，银行仍是箱子界面，领地命令也照常可用。

想要界面的人，把对应的 jar 放进**自己的客户端** `mods` 文件夹。不要把它们当成服务器核心，也不要只装界面、不在服务器上装核心。

| 客户端 jar | 作用 |
| --- | --- |
| `bankmod-ui-1.0.0.jar` | 可选的银行界面。装上之后不再打开箱子界面。 |
| `landmod-ui-1.0.0.jar` | 可选的领地界面。可以在地图上选取整块区块。 |

两个界面都没有自定义物品或方块，也不注册会改变原版握手的内容。没装界面的人和原版客户端不受影响。

商店界面在 `shop-ui`，打出来的是 `shopmod-ui-1.0.0.jar`。市场界面的文件夹 `market-ui` 还留着，现在不打 jar。

The dedicated server only needs the core `bankmod` and `landmod` jars, plus Fabric Loader and Fabric API. Players can join with a vanilla client. Without the UI jars, the bank stays a chest screen and the land commands still work.

People who want the screens put `bankmod-ui-1.0.0.jar`, `landmod-ui-1.0.0.jar`, or `shopmod-ui-1.0.0.jar` in their own client `mods` folder. These jars add no custom items or blocks. `market-ui` is still reserved and is not built.

## 银行界面

按 `K` 打不开银行。走到网点，用原来的方式打开柜台。客户端如果装了这个 jar，会告诉服务器改用这个界面，服务器就不再发箱子。

界面没有侧边栏，也没有贴在左右两边的页签。页签在底部：储蓄、存入、取出、兑换、汇票、交易、个人发行。管理员另外有「管理」。

上方单独写着：

- M0、M1、M2，各保留 6 位小数
- 汇率，单位是铜锭每钻石
- 正常的兑付状态，例如「兑付正常」「暂停兑付」「无法兑付」「准备金不足」「准备金紧张」

不显示「兑付能力」这一类多余的兑付说明，也不重复写「兑付：正常」。

存入和取出用 `1`、`5`、`10`、`64` 和「全部」。界面上看到的是中文名称，不显示指令编号。

当前银行核心发来的数据里如果还没有汇率数字，这一行会写成「汇率 —」，状态行仍然保留。核心以后把汇率放进同一份数据时，界面会直接显示。

There is no sidebar. Page buttons sit along the bottom. The lines above the page are M0, M1, and M2 to 6 decimal places, the copper-per-diamond exchange rate, and the normal redemption status. Extra redemption wording is not shown. Deposit and withdraw use the buttons 1, 5, 10, 64, and 全部. If the current bank core has not sent a rate yet, the rate line reads「汇率 —」and the status line stays.

## 领地界面

默认按 `K` 打开。不打开这个界面也能用原来的 `/land` 命令。

每块都是完整的 16×16 区块。宗地由这样的整块组成。斜对角不算相邻。可以按「整宗地」并入脚下相邻的宗地，也可以按「拆出区块」标记脚下的区块再拆成新宗地。人没站在那一块里时，界面不会假装已经买下，服务器仍按命令判断。

装了 Xaero's World Map 时，世界地图上方有「选取此区块」，右键地图也有同样的一项。装了 Xaero's Minimap 时，从小地图打开路标界面，上面也有「选取此区块」。选出的是光标所在的整块区块。这两个地图模组都不是必须的。没装时，领地命令一样能用。

如果客户端同时还装了完整的 `landmod`，地图上色仍由 `landmod` 负责，这个 jar 负责选取和界面。只有这个 jar、没有 `landmod` 客户端时，它自己接收领地数据，并在装了地图的时候给已占领的区块上色。你的领地是绿色，别人的是橙色。

Press K to open the land screen. `/land` still works if you never open it. Selection on Xaero's World Map or Minimap is always a whole 16×16 chunk. Whole-parcel join and split-chunk both use the commands landmod already has. The server decides whether the action is allowed. The map mods are optional.

## 构建

需要 Java 17 或更高版本来运行 Gradle。编译目标是 Java 17。

```
./gradlew test remapJar
```

给玩家用的是重映射后的 jar：

- `bank-ui/build/libs/bankmod-ui-1.0.0.jar`
- `land-ui/build/libs/landmod-ui-1.0.0.jar`

不要用带 `-dev` 或 `-sources` 的文件。

Java 17 is the bytecode target. `./gradlew test remapJar` runs the tests and writes the two remapped jars listed above.

## 许可

MIT License. Copyright (c) 2026 heplus1s.
