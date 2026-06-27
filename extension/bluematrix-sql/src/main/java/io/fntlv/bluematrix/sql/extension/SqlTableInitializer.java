package io.fntlv.bluematrix.sql.extension;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.SQLTable;
import io.fntlv.bluematrix.core.module.ModuleContext;
import io.fntlv.bluematrix.sql.core.BlueDatabase;

import java.sql.SQLException;
import java.util.Set;

public class SqlTableInitializer {

    public void initialize(ModuleContext context, BlueDatabase database) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null");
        }

        SQLManager sqlManager = database.sqlManager();
        Set<Class<? extends SQLTable>> tableTypes = context.getReflections().getSubTypesOf(SQLTable.class);
        for (Class<? extends SQLTable> tableType : tableTypes) {
            if (!tableType.isEnum()) {
                continue;
            }
            createTables(context, sqlManager, tableType);
        }
    }

    private void createTables(ModuleContext context, SQLManager sqlManager, Class<? extends SQLTable> tableType) {
        SQLTable[] tables = tableType.getEnumConstants();
        if (tables == null) {
            return;
        }
        for (SQLTable table : tables) {
            try {
                table.create(sqlManager);
            } catch (SQLException e) {
                throw new SqlTableInitializationException("Module SQL table initialization failed: "
                        + context.id() + " (" + tableType.getName() + ")", e);
            }
        }
    }
}
