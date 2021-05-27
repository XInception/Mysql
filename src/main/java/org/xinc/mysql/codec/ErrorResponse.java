package org.xinc.mysql.codec;


public class ErrorResponse extends AbstractMySqlPacket implements MysqlServerPacket {

	private final int errorNumber;
	private final byte[] sqlState;
	private final String message;

	public ErrorResponse(int sequenceId, int errorNumber, byte[] sqlState, String message) {
		super(sequenceId);
		this.errorNumber = errorNumber;
		this.sqlState = sqlState;
		this.message = message;
	}

	public int getErrorNumber() {
		return errorNumber;
	}

	public byte[] getSqlState() {
		return sqlState;
	}

	public String getMessage() {
		return message;
	}

}
