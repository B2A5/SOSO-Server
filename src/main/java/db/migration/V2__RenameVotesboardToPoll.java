package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * votesboard 테이블 및 컬럼명을 polls 도메인 네이밍으로 마이그레이션.
 *
 * - 테이블: votesboard → polls
 * - 컬럼:  end_time → closed_at
 *           allow_revote → can_revote
 *           allow_multiple_choice → can_multi_select
 *           total_votes → participant_count
 *
 * 조건부 실행: votesboard 테이블이 존재하는 경우에만 rename 수행.
 * 이미 polls로 이름이 변경된 경우(fresh DB 등)에는 아무것도 하지 않음.
 */
public class V2__RenameVotesboardToPoll extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();

        // 1. votesboard 테이블이 존재하는지 확인
        boolean votesboardExists = tableExists(meta, catalog, "votesboard");
        if (!votesboardExists) {
            // 이미 마이그레이션됐거나 fresh DB — 아무것도 하지 않음
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            // 2. 테이블 rename: votesboard → polls
            stmt.execute("RENAME TABLE votesboard TO polls");

            // 3. 컬럼 rename (MySQL 8.0.4+ 지원)
            stmt.execute("ALTER TABLE polls RENAME COLUMN end_time TO closed_at");
            stmt.execute("ALTER TABLE polls RENAME COLUMN allow_revote TO can_revote");
            stmt.execute("ALTER TABLE polls RENAME COLUMN allow_multiple_choice TO can_multi_select");
            stmt.execute("ALTER TABLE polls RENAME COLUMN total_votes TO participant_count");
        }
    }

    private boolean tableExists(DatabaseMetaData meta, String catalog, String tableName) throws Exception {
        try (ResultSet rs = meta.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
