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
import reservation.CheckOut;
import reservation.Check;
import utilitaire.UtilDB;
import utilitaire.Utilitaire;
import user.UserEJB;
import bean.ClassMAPTable;
import utils.ConstanteLocation;
import annexe.Point;

@Path("/checkout")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckoutWS {

    @GET
    @Path("/reception-formulaire")
    public Response getReceptionFormulaire(@QueryParam("idresa") String idresa) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            // ✅ Récupérer la réservation et les check-ins comme dans checkout-saisie.jsp
            Reservation mere = new Reservation();
            mere.setId(idresa);
            
            // ✅ Utiliser la même méthode que dans le JSP : getListeCheckIn("CHECKOUT_DATERETOUR")
            Check[] res = mere.getListeCheckIn("CHECKOUT_DATERETOUR", c);
            
            if (res == null || res.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("erreur", true);
                error.put("message", "Aucun check-in trouvé pour cette réservation");
                return Response.ok(error).build();
            }
            
            int nombreLigne = res.length;
            
            // ✅ Préparer les données comme dans le JSP
            List<Map<String, Object>> lignes = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String dateAujourdhui = Utilitaire.dateDuJour();
            String heureActuelle = Utilitaire.heureCouranteHM();
            
            for (int i = 0; i < nombreLigne; i++) {
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("reservation", res[i].getId());
                ligne.put("produit", res[i].getProduitLibelle());
                ligne.put("refproduit", res[i].getRefproduit());
                ligne.put("quantite", (int) res[i].getQte());
                ligne.put("daty", res[i].getDaty() != null ? sdf.format(res[i].getDaty()) : dateAujourdhui);
                ligne.put("heure", heureActuelle);
                ligne.put("idmagasin", ConstanteLocation.magasinAtioik);
                ligne.put("responsable", "");
                ligne.put("retenue", 0);
                ligne.put("jour_retard", 0);
                ligne.put("etat_materiel", "0");
                ligne.put("remarque", "");
                lignes.add(ligne);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("erreur", false);
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
    @Path("/reception-valider")
    public Response validerReception(Map<String, Object> data, @Context HttpServletRequest request) {
        try {
            // ✅ Récupérer l'utilisateur depuis la session comme dans apresMultiple.jsp
            HttpSession session = request.getSession();
            UserEJB u = (UserEJB) session.getAttribute("u");
            if (u == null || u.getUser() == null) {
                return Response.status(401)
                    .entity("{\"error\":\"Utilisateur non authentifié\"}")
                    .build();
            }
            
            // ✅ Récupérer les données
            String idreservation = (String) data.get("idreservation");
            List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");
            
            // ✅ Créer un tableau de CheckOut comme dans apresMultiple.jsp (acte=insertFilleSeul)
            CheckOut[] cfille = new CheckOut[lignes.size()];
            
            for (int i = 0; i < lignes.size(); i++) {
                Map<String, Object> ligne = lignes.get(i);
                CheckOut checkout = new CheckOut();
                checkout.setNomTable("CHECKOUT");
                
                // ✅ Remplir les champs comme dans checkout-saisie.jsp
                checkout.setReservation((String) ligne.get("reservation"));
                checkout.setDaty(Utilitaire.string_date("dd/MM/yyyy", (String) ligne.get("daty")));
                checkout.setHeure((String) ligne.get("heure"));
                
                // ✅ IMPORTANT : Arrondir les valeurs numériques avant insertion (comme dans le JS)
                checkout.setQuantite(Math.floor(Double.parseDouble(ligne.get("quantite").toString())));
                checkout.setRetenue(Math.floor(Double.parseDouble(ligne.get("retenue").toString())));
                
                checkout.setResponsable((String) ligne.get("responsable"));
                checkout.setIdmagasin((String) ligne.get("idmagasin"));
                checkout.setRemarque((String) ligne.get("remarque"));
                
                cfille[i] = checkout;
            }
            
            // ✅ Utiliser EXACTEMENT la même méthode que dans apresMultiple.jsp : createObjectFilleMultipleSansMere
            // ⚠️ PAS DE CONNEXION PASSÉE, la méthode gère tout en interne
            u.createObjectFilleMultipleSansMere(cfille);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Réception enregistrée avec succès");
            return Response.ok(result).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        }
    }
}