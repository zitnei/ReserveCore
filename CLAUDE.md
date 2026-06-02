\# ReserveCore 開発ルール



\## プロジェクト名

ReserveCore



\## 目的

未経験バックエンド志望者が企業に提出できる、実務型の予約・売上・在庫管理APIを作る。



\## Claude Codeの役割

あなたはこのプロジェクトのメイン開発AIです。

実装、修正、テスト、リファクタリング、Git管理を担当します。



\## 他AIの役割

\- ChatGPT: 設計レビュー、DB設計、README、採用担当目線の改善

\- Gemini: 仕様の抜け漏れ、業務フロー、例外パターン確認

\- GitHub Copilot: 小さいコード補完

\- GitHub Actions: 自動テスト

\- Claude Code Action: PRレビュー、Issue対応



\## 技術スタック

\- Java 17

\- Spring Boot

\- PostgreSQL

\- Docker

\- Docker Compose

\- JPA / Hibernate

\- Spring Security

\- JWT

\- JUnit

\- GitHub Actions



\## 開発ルール

\- いきなり全機能を作らない

\- まずMVPを完成させる

\- 1ステップずつ進める

\- ファイル名と配置場所を必ず書く

\- 変更前に実装計画を出す

\- 実装後にテスト方法を書く

\- エラーが出たら原因と修正方法を書く

\- セキュリティと権限管理を重視する

\- 採用担当が見ても理解しやすいREADMEにする



\## MVP

最初に作る機能:

1\. ユーザー登録

2\. ログイン

3\. JWT認証

4\. 権限管理

5\. 予約登録

6\. 予約一覧

7\. 予約キャンセル

8\. PostgreSQL接続

9\. Docker Compose

10\. JUnitテスト



\## 禁止事項

\- 大量のファイルを一気に作らない

\- 説明なしで大規模変更しない

\- 動かないコードを出さない

\- TODOだけで終わらせない

