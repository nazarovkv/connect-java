package cd.connect.aws.rds

import com.amazonaws.services.rds.model.DBInstance

/**
 * Created by Richard Vowles on 4/04/18.
 */
interface CreateInstanceResult {
	void result(boolean success, DBInstance instance)
}
