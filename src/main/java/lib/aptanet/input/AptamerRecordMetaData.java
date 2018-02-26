/**
 * 
 */
package lib.aptanet.input;

import java.net.URI;

import org.datavec.api.records.metadata.RecordMetaData;

/**
 * @author Jan Hoinka
 * Metadata from an aptamer instance
 *
 *
 */
public class AptamerRecordMetaData implements RecordMetaData {

	private String location;
	private URI uri;
	private Class<?> c;
	
	
	public AptamerRecordMetaData(String location, URI uri, Class<?> c) {
		
		this.location = location;
		this.uri = uri;
		this.c = c;
		
	}
	
	/* (non-Javadoc)
	 * @see org.datavec.api.records.metadata.RecordMetaData#getLocation()
	 */
	@Override
	public String getLocation() {
		return location;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.metadata.RecordMetaData#getURI()
	 */
	@Override
	public URI getURI() {
		return uri;
	}

	/* (non-Javadoc)
	 * @see org.datavec.api.records.metadata.RecordMetaData#getReaderClass()
	 */
	@Override
	public Class<?> getReaderClass() {
		return c;
	}

}
