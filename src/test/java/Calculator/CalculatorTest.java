package Calculator;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private Calculator calculator;

    @BeforeAll
    static void initAll(){
        System.out.println("Before All");
    }

    @BeforeEach
    void init(){
        calculator = new Calculator();

        calculator.rand = Mockito.mock(Calculator.Rand.class);
        Mockito.when(calculator.rand.getRand())
                .thenReturn(698);
    }

    @AfterEach
    void tearDown(){
        calculator = null;
        System.out.println("After");
    }

    @AfterAll
    static void tearDownAll(){
        System.out.println("After All");
    }

    @Test
    void sum() {
        assertEquals(15f, calculator.sum(8f, 7f), "Failed 1");
    }

    @Test
    void mult() {
        assertEquals(15f, calculator.mult(3f, 5f), "Failed 2");
    }

    @Test
    void divide() {
        assertEquals(5f, calculator.divide(100, 20), "Failed 3");
    }

    @Test
    @DisplayName("Degree Test")
    @Disabled("Ignore Test #1")
    void degree() {
        assertEquals(16f, calculator.degree(2f, 4), "Failed 4");
    }

    @Test
    @DisplayName("Random Test")
    void getRand(){
        assertEquals(698, calculator.rand.getRand());
    }
}