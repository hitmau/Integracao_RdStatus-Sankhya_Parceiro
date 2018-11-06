import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
//import br.com.sankhya.mgecomercial.listeners.NativeSql;

public class RdStationParceiros implements AcaoRotinaJava {
	String pId, pEmail, pCodpap, pNomePap, pNomeCtt, pCttNDec, pTipCham, pComentarios, pIdCtt, pOportunidade, pAgendaMKT;
	Integer pNurel, pCountC = 0, pCountA = 0, pSegundos = 10, pUser, linhas = 50;
	Calendar segundos = Calendar.getInstance();
	
	
	
	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) {
	}
	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub
		
		//mensagem de alerta ao final do processo
		StringBuffer mensagem = new StringBuffer();
		
		//Obtemos uma consulta para buscar os lançamentos
		QueryExecutor query = contexto.getQuery();
		
		//Obtem o usuário logado
		pUser = contexto.getUsuarioLogado().intValue();
			
        query.nativeSelect("SELECT AD.ID   " +
        		", AD.CODPAP     " +
        		", AD.IDCTT    " +
        		", AD.OPORTUNIDADE   " +
        		", AD.AGENDAMKT   " +
        		", (SELECT PAR.NOMEPARC FROM TGFPAR PAR WHERE AD.CODPAP=PAR.CODPARC) AS NOMEPROSPECT   " +
        		", (SELECT CTT.NOMECONTATO FROM TGFCTT CTT WHERE CTT.CODPARC = AD.CODPAP AND CTT.CODCONTATO = AD.IDCTT) AS NOMECONTATO   " +
        		", AD.CTTNDECISOR AS CTTND   " +
        		", (SELECT CTT.EMAIL FROM TGFCTT CTT WHERE AD.CODPAP=CTT.CODPARC AND CTT.CODCONTATO = AD.IDCTT) AS EMAIL   " +
        		", (SELECT ID.DESCRICAO FROM AD_IDENTIFICADORESRD ID WHERE ID.IDMASC = AD.IDMASC) AS IDENDIFICADOR  " +
        		"FROM AD_CONVMKTDIGITAL AD   " +
        		"WHERE NVL(AD.ENVIO, 'N') = 'N'  " +
        		"  AND AD.CODUSU IN (SELECT DISTINCT CASE WHEN (SELECT U.CODGRUPO FROM TSIUSU U WHERE U.CODUSU IN " + this.pUser + ") IN (12,13, 1)   " +
        		"	THEN (SELECT distinct U.CODUSU FROM TSIUSU U WHERE U.CODUSU=USU.CODUSU)   " +
        		"	ELSE (SELECT U.CODUSU FROM TSIUSU U WHERE U.CODUSU = " + this.pUser + ") END   " +
        		"	FROM TSIUSU USU)  " +
        		" AND NVL(AD.REMOVER, 'N') <> 'S' " +
        		" AND AD.TABELA = 'tgfpar'");
        
        while(query.next())
		{

			this.pOportunidade = query.getString("OPORTUNIDADE");
			this.pCodpap = query.getString("CODPAP");
			this.pNomePap = query.getString("NOMEPROSPECT");
			this.pNomeCtt = query.getString("NOMECONTATO");
			this.pCttNDec = query.getString("CTTND");
			this.pEmail = query.getString("EMAIL");
			this.pId = query.getString("IDENDIFICADOR");
			this.pIdCtt = query.getString("IDCTT");
			this.pAgendaMKT = query.getString("AGENDAMKT");

			if (this.pOportunidade == "S") {
				this.pOportunidade = "true";
			} else {
				this.pOportunidade = "false";
			}


			//ENVIO DOS DADOS NO LOOP
			//envio para o RD
			
			RdStationParceiros http = new RdStationParceiros();

			http.sendPost();

			String url = "https://www.rdstation.com.br/api/1.3/conversions";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			//add reuqest header
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			String urlParameters = "token_rdstation=&email="  + this.pEmail + 
					"&identificador=" + this.pId +
					"&nome=" + this.pNomeCtt +
					"&empresa=" + this.pNomePap +
					"&contato-nao-decisor=" + this.pCttNDec;

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'POST' request to URL : " + url);
			System.out.println("Post parameters : " + urlParameters);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			//Fim envio para o rd
			
			//UPDATE DAS TABELAS APOS ENVIO DOS DADOS PARA O RD
/* linha é para o prospect
			query.update("UPDATE TCSPAP SET " +
					"	          AD_PAPVALIDO = 'S'" +
					"			, AD_DTINIFLUXO = SYSDATE" +
					"			, AD_ATIVIDADESFLUXO = 2" +
					"			, AD_FLUXOLEAD = 2 " +
					"		WHERE CODPAP = " + this.pCodpap);
*/
			query.update("UPDATE AD_CONVMKTDIGITAL AD SET " +
					"			   AD.ENVIO = 'S'" +
					"			 , AD.DTHRREGISTRO = SYSDATE " +
					"	   WHERE CODPAP = " + this.pCodpap + " AND TABELA = 'tgfpar'");

			pCountC++;
			pSegundos++;
			//int i = 1;
			//I para inserir na agenda
			if (this.pAgendaMKT.equals("I")) { 
				Calendar hoje = Calendar.getInstance();
				DateFormat dateFormat = new SimpleDateFormat("MM");
				Date date = new Date();
				int mesatual = Integer.parseInt(dateFormat.format(date));
				int antes = 0;
				for (int i = 1 ; i < 4 ; i++){
					pCountA++;
					segundos.add(Calendar.SECOND, + 1);
					pSegundos = pSegundos + 1;//segundos.get(Calendar.HOUR) + i;

					hoje.add(Calendar.DATE, +2);
					
					pTipCham = "A";
					pComentarios = "(Ligações) Registrado automaticamente.";
					
					Registro mkt = contexto.novaLinha("TGFTEL");
						mkt.setCampo("CODPARC", this.pCodpap);
						mkt.setCampo("DHCHAMADA", getDateTime() + ":" + pSegundos); //getDateTime()
						mkt.setCampo("COMENTARIOS", "Parceiro: " + this.pCodpap + " - " +this.pNomePap+ " | Contato: " + this.pNomeCtt + " | Follow Up " + i + " - Fluxo automático." );
						mkt.setCampo("COMENTARIOS2", "(Ligação " + i + ")"); //this.pComentarios
						
						if (antes > hoje.get(Calendar.DATE)) {
							if (antes == 12) {
								mesatual = 1;
							} else {
								mesatual = mesatual + 1;
							}
						}
						antes = hoje.get(Calendar.DATE);
						
						mkt.setCampo("DHPROXCHAM",  hoje.get(Calendar.DATE) + "/" + mesatual + "/" + hoje.get(Calendar.YEAR) + " 10:" + getDateTimeMin() + ":" + pSegundos); //PROXIMA CHAMADA
						mkt.setCampo("PENDENTE", "S");
						mkt.setCampo("CODUSU", this.pUser); //this.pUser
						mkt.setCampo("SITUACAO", "P");
						mkt.setCampo("CODATENDENTE", this.pUser); //this.pUser
						mkt.setCampo("DTALTER", getDateTime());
						//mkt.setCampo("AD_CODPAP", this.pCodpap);
						mkt.setCampo("CODCONTATO", this.pIdCtt);
						mkt.setCampo("TIPCHAM", this.pTipCham);
						mkt.setCampo("AD_REGMKTDIGITAL", "S");
						mkt.setCampo("AD_FALLOWUP", "F"+i);
						mkt.setCampo("CODPROD", 0);
					mkt.save();
					hoje.add(Calendar.DATE, +2);
				}
			} //Fim do for

		} //Fim do while

		query.close();

		mensagem.append("Atualização realizada!\n");
		
		if (pCountC.equals(0) && pCountA.equals(0)) {
			mensagem.append("Nenhum registro na agenda foi modificado!\n");
		} else {
			mensagem.append("Total de (" + pCountC + ") registro(s) enviado(s) e (" + this.pCountA + ") agendas registradas.\n" + getDateTime() + ":" + pSegundos);
		}
			//mensagem.append("Cód. usuário: " + this.pUser);
		contexto.setMensagemRetorno(mensagem.toString());
	}

	private String getDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		Date date = new Date();
		return dateFormat.format(date);
	}
	private String getDateTimeMin() {
		DateFormat dateFormat = new SimpleDateFormat("mm");
		Date date = new Date();
		return dateFormat.format(date);
	}

	private void sendPost() {
		// TODO Auto-generated method stub
	}
}