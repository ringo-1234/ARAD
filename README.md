# ARAD

[English](README.en.md) | [日本語](README.md)

[Minecraft](https://www.minecraft.net/ja-jp) | [forge1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)

# 最新版のダウンロードは[こちら](https://github.com/ringo-1234/ARAD/releases/latest) から

## この Mod はなにか

ARAD は RealTrainMod(RTM),NGTLib,MCTE の非公式拡張物です。

## 注意事項

**当 Mod の使用による一切の責任を負いません。**
**AppleExtended-2.4.2以降のバージョンが必須です。**

## 導入方法

当 Mod を DL し、mods フォルダに入れてください

## 使用方法

まず、前提として、マップが利用できます、また、駅ブロック,制限ブロックの2つブロックが追加されます。
駅ブロックにAppleExtended内蔵のARTPEによる編成アイテムをセットし、マップから駅を順に選択して路線を作ることで最初に選択された駅ブロックの中に入っている編成アイテムの編成を呼び出し、終点駅まで順々に停車します。

## 知っておいたら便利

・このModはそれぞれの編成に毎tick処理を施すのでTPSの低下は体感で感じられる場合もあります。
・このModの処理はレールという概念を把握していないので、編成が別の方向にいってしまっても編成からは対応ができません。
(レールという概念を入れると先のレールを読み込む処理が出てきてしまうため大幅なTPS低下が見られるためです。)
・現段階では始点駅でスポーンし、終点駅でデスポーンするという処理です、気になる場合は始点終点を車庫にすることをお勧めします。

## 謝辞

公開にあたり、配布許可を頂いた[NGT-5479](https://twitter.com/ngt5479) 様に、この場をお借りして感謝申し上げます。
