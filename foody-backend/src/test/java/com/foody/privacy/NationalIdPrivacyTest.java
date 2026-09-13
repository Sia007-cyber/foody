package com.foody.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.businesses.dto.BusinessResponse;
import com.foody.users.dto.UserResponse;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class NationalIdPrivacyTest {

    @Test
    void publicBusinessAndUserDtosDoNotExposeManagerNationalId() {
        assertThat(Arrays.stream(BusinessResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("managerNationalId");
        assertThat(Arrays.stream(UserResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("managerNationalId");
    }
}
