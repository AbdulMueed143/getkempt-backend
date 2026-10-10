package au.com.getkempt.controllerTests;


import au.com.getkempt.controllers.staff.StaffController;
import au.com.getkempt.services.staff.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class StaffControllerTests {

    //To perform this test we need following
    //We need staff service, mock it
    @Mock
    private StaffService staffService;


    //The mocks we have created above should be moved to the controller we are going to be testing
    @InjectMocks
    private StaffController staffController;


    @BeforeEach
    public void setup() {

    }

    //Happy Case
    @Test
    void shouldFindStaffMember_whenProvidedWithItsId() {

    }

    @Test
    void shouldNotFindStaffMember_whenProvidedWithWrongId() {

    }


    @Test
    void shouldCreateStaffMember() {

    }

    @Test
    void shouldFailWithValidationErrors_whenISendIncorrectData() {

    }


}
