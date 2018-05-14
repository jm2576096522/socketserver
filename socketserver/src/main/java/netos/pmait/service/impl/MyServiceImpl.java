package netos.pmait.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import netos.pmait.dao.HisDeviceMapper;
 
import netos.pmait.service.MyServiceI;

@Service("myServiceI")
public class MyServiceImpl implements MyServiceI {

	@Autowired
	public HisDeviceMapper mHisDeviceMapper;

	public void getMlist() {

	}

	/*public int bandDevice(DataPacket device) {

		return 0;
	}*/

 
	/*public HisDevice queryDeviceInfo(String mac) {
		 
		return mHisDeviceMapper.queryDeviceInfo(mac);
	}

 
	public int insertDeviceMac(HisDevice record) {
		 
		try {
			return mHisDeviceMapper.insertDeviceMac(record);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

 
	public int updateDeviceInfo(HisDevice record) {
		 
		try {
			if(queryDeviceInfo(record.getMac())!=null){
				return mHisDeviceMapper.updateDeviceInfo(record);
			}else {
				return insertDeviceMac(record);
			}
			
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}*/

}
