package uncompiled;

public class SubClass extends ParentClass {

    @Override
    protected void testMethod() {
        System.out.println(SOME_ID);
    }

    @Override
    protected void testMethod2() {
        System.out.println(SOMETHING_ELSE);
    }
}
