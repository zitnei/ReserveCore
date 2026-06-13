# デプロイ手順 (本番環境)

ブラウザ上で実際に動作確認できる状態にするためのデプロイ手順です。

- **API (バックエンド + DB)**: Render
- **フロントエンド**: Vercel
- **構成図**:
  ```
  [ブラウザ] → Vercel (Next.js)
                    │  fetch
                    ▼
              Render Web Service (Spring Boot)
                    │  JDBC
                    ▼
              Render PostgreSQL 15
  ```

---

## 1. バックエンドを Render にデプロイ

### 1-1. リポジトリを GitHub に push

```bash
git push origin master
```

### 1-2. Render Blueprint で一括作成

1. [Render Dashboard → Blueprints](https://dashboard.render.com/blueprints) を開く
2. **New Blueprint Instance** をクリック
3. このリポジトリ (`ReserveCore`) を選択
4. `render.yaml` が自動検出される → **Apply**
5. 以下が同時に作成される:
   - `reservecore-db` (PostgreSQL 15, Free プラン)
   - `reservecore-api` (Web Service, Docker, Free プラン)

### 1-3. シークレットを Render UI で入力

Blueprint apply 後、`reservecore-api` の **Environment** タブで以下を設定:

| Key | 値 | 備考 |
|---|---|---|
| `ADMIN_PASSWORD` | 強固な12文字以上 | 初期管理者のパスワード |
| `CORS_ALLOWED_ORIGINS` | (Vercel デプロイ後に入力) | 例: `https://reservecore-frontend.vercel.app` |

### 1-4. 動作確認

```bash
# ヘルスチェック
curl https://reservecore-api.onrender.com/actuator/health
# → {"status":"UP"}

# ADMIN ログイン
curl -X POST https://reservecore-api.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@reservecore.com","password":"<設定したパスワード>"}'
```

> ⚠️ Render Free プランは **15分アクセスがないとスリープ**します。
> 初回アクセスは 30秒〜1分待たされる旨を README に明記しています。

---

## 2. フロントエンドを Vercel にデプロイ

別リポジトリ `ReserveCore-frontend` を作成後、以下の手順:

### 2-1. リポジトリを GitHub に push

### 2-2. Vercel で Import

1. [Vercel Dashboard](https://vercel.com/new) → **Add New Project**
2. `ReserveCore-frontend` を選択
3. **Environment Variables** に以下を追加:
   | Key | Value |
   |---|---|
   | `NEXT_PUBLIC_API_BASE_URL` | `https://reservecore-api.onrender.com` |
4. **Deploy**

### 2-3. デプロイ URL を Render の CORS_ALLOWED_ORIGINS に反映

Vercel の本番 URL (例: `https://reservecore-frontend.vercel.app`) を
Render の `reservecore-api` の `CORS_ALLOWED_ORIGINS` に設定して **Save Changes**。
→ Render が自動で再デプロイされる。

---

## 3. デプロイ後チェックリスト

- [ ] `https://reservecore-api.onrender.com/actuator/health` が `UP`
- [ ] Vercel フロントから ADMIN でログインできる
- [ ] 店舗作成 → サービス作成 → スタッフ割当 → 予約作成まで通る
- [ ] 二重予約で 409 が返る
- [ ] CUSTOMER で他人の予約が見えない (403 ではなく自分のだけ表示)
- [ ] README のデモ URL を本番 URL に更新

---

## 4. Free プランの制約と対策

| 制約 | 対策 |
|---|---|
| Render Web Service が 15分でスリープ | README で「初回アクセスは30秒待ち」と告知 |
| Render Free Postgres は 90日で失効 | 失効前に新規 DB 作成 → Flyway で再構築 (V1〜V3 が自動適用) |
| Vercel は商用利用に制限あり | ポートフォリオ用なので問題なし |

---

## 5. ローカル開発時の CORS

ローカル開発では `CORS_ALLOWED_ORIGINS` を未設定にすれば
デフォルトの `http://localhost:3000` (Next.js dev server) が許可される。
`docker compose up` でそのまま動作する。
