/**
 * 
 */

/**
 * @author acton
 *
 */
public class DNSLookupsExceededException extends Exception {
	private int lookupCount = 0;
	DNSLookupsExceededException(int cnt) {
		lookupCount = cnt;
	}
	
	int lookupCount() {
		return lookupCount;
	}
}
