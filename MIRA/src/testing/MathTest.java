package testing;

import org.junit.Test;

public class MathTest {
	@Test
	public void logTest() {
		System.out.println(Math.log(0));
		System.out.println(Math.log(1));
		System.out.println(Math.log(-0.1));
	}
}
