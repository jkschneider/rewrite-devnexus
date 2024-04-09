package com.devnexus.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class SpringProfiles extends DataTable<SpringProfiles.Row> {

    public SpringProfiles(Recipe recipe) {
        super(recipe, "Spring profiles", "A list of Spring profiles.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Profile",
                description = "The profile name.")
        String profileName;
    }
}
