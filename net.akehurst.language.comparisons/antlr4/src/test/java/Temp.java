public class Temp {

    @interface An1 {}

    @interface An2 {
        An1[] value();
    }

    @An2(@An1)
    interface Intf {  }



}



//@An(@An(0))
//interface Intf {  }