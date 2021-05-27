
package org.xinc.mysql.codec;

abstract class AbstractMySqlPacket implements MysqlPacket {

	private int sequenceId;

	public AbstractMySqlPacket(int sequenceId) {
		this.sequenceId = sequenceId;
	}

	@Override
	public int getSequenceId() {
		return this.sequenceId;
	}

	@Override
	public void setSequenceId(int sequenceId){
		this.sequenceId=sequenceId;
	};

}
