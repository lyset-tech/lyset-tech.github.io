class T{
    private void test() {
       /*Long l = 0L;

       switch (l){
          case 1:
          case 0:
	  case 2:
            System.out.println(l);
        }*/
    }
    private void test1(){
	String feiniao = "feiniao";
	switch(feiniao){
		case "FB":
			System.out.println("FB");
			break;
		case "Ea":
			System.out.println("Ea");
			break;
	}
    } 
    private void test2(){
       Em em = Em.E;
       switch (em){
       		case A:
             System.out.println("A");
             break;
          case C:
             System.out.println("C");
             break;
          case E:
    		     System.out.println("E");
             break;
          default:

       }
    }
}
enum Em {
    A,B,C,D,E;
}
