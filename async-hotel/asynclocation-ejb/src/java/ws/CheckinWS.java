package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

import reservation.Reservation;
import reservation.ReservationLib;
import reservation.CheckInLib;
import reservation.ReservationDetailsCheck;
import utilitaire.UtilDB;
import bean.CGenUtil;
import bean.ClassMAPTable;

@Path("/checkin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckinWS {

    @GET
    @Path("/livraison-formulaire")
    public Response getLivraisonFormulaire(@QueryParam("idresa") String idresa) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            Reservation mere = new Reservation();
            mere.setId(idresa);

            // Récupérer les détails de la réservation sans checkin
            ReservationDetailsCheck[] res = mere.getListeSansCheckIn("RESTSANSCIGROUPLIB_SANS", c);
            
            if (res == null || res.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("erreur", true);
                error.put("message", "Aucun détail de réservation trouvé");
                return Response.ok(error).build();
            }
            
            int nombreLigne = res.length;

            // Récupérer les infos client et réservation
            ReservationLib t = new ReservationLib();
            t.setNomTable("RESERVATION_LIB_MIN_DATYF");
            ReservationLib[] liste = (ReservationLib[]) CGenUtil.rechercher(t, null, null, c, " AND ID='" + idresa + "'");
            boolean erreur = false;
            if (liste.length > 0) {
                if (liste[0].getResteAPayer() > 0) {
                    erreur = true;
                }
            }

            // Préparer le formulaire à retourner
            List<Map<String, Object>> lignes = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            for (int i = 0; i < nombreLigne; i++) {
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("idproduit", res[i].getIdproduit());
                ligne.put("referenceproduit", res[i].getReferenceproduit());
                ligne.put("idclient", res[i].getIdclient());
                ligne.put("idclientlib", res[i].getIdclientlib());
                ligne.put("reservation", res[i].getId());
                ligne.put("qte", res[i].getQtearticle());
                ligne.put("daty", sdf.format(res[i].getDaty()));
                ligne.put("responsable", mere.getResponsable());
                ligne.put("image", res[i].getImage());
                ligne.put("type", res[i].getType());
                ligne.put("qteUtil", res[i].getQteUtil());
                ligne.put("magasin", res[i].getMagasin());
                ligne.put("produitlib", res[i].getProduitlib());
                lignes.add(ligne);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("erreur", erreur);
            result.put("lignes", lignes);
            result.put("nombreLigne", nombreLigne);
            result.put("idreservation", idresa);
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }

    @POST
    @Path("/livraison-valider")
    public Response validerLivraison(Map<String, Object> data, @Context HttpServletRequest request) {
        Connection c = null;
        try {
            // ✅ Récupérer l'utilisateur depuis la session comme dans le JSP
            HttpSession session = request.getSession();
            user.UserEJB u = (user.UserEJB) session.getAttribute("u");
            if (u == null || u.getUser() == null) {
                return Response.status(401)
                    .entity("{\"error\":\"Utilisateur non authentifié\"}")
                    .build();
            }
            
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            // Récupérer les données du formulaire
            String idreservation = (String) data.get("idreservation");
            List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");

            // ✅ Créer un tableau de CheckInLib comme dans apresMultipleCheckin.jsp
            CheckInLib[] cfille = new CheckInLib[lignes.size()];
            
            for (int i = 0; i < lignes.size(); i++) {
                Map<String, Object> ligne = lignes.get(i);
                CheckInLib checkin = new CheckInLib();
                checkin.setNomTable("CHECKIN");
                
                // ✅ Remplir les champs comme dans le JSP
                checkin.setIdProduit((String) ligne.get("idproduit"));
                checkin.setIdClient((String) ligne.get("idclient"));
                checkin.setReservation((String) ligne.get("reservation"));
                checkin.setQte(Double.parseDouble(ligne.get("qte").toString()));
                checkin.setDaty(utilitaire.Utilitaire.string_date("dd/MM/yyyy", (String) ligne.get("daty")));
                checkin.setResponsable((String) ligne.get("responsable"));
                checkin.setImage((String) ligne.get("image"));
                
                cfille[i] = checkin;
            }
            
            // ✅ Utiliser la même méthode que dans apresMultipleCheckin.jsp
            Reservation reservation = new Reservation();
            reservation.createObjectFilleMultipleSansMere(u, cfille);
            
            c.commit();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Livraison enregistrée avec succès");
            return Response.ok(result).build();
        } catch (Exception e) {
            if (c != null) try { c.rollback(); } catch (Exception ignore) {}
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }
}