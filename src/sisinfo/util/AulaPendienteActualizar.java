package sisinfo.util;

import java.sql.Timestamp;

public class AulaPendienteActualizar {
	
	private String idAulaSisinfo;
	private Timestamp fechaNovedadDesde;

	public AulaPendienteActualizar(){}

	public String getIdAulaSisinfo() {
		return idAulaSisinfo;
	}

	public void setIdAulaSisinfo(String idAulaSisinfo) {
		this.idAulaSisinfo = idAulaSisinfo;
	}

	public Timestamp getFechaNovedadDesde() {
		return fechaNovedadDesde;
	}

	public void setFechaNovedadDesde(Timestamp fechaNovedadDesde) {
		this.fechaNovedadDesde = fechaNovedadDesde;
	}
	
}
