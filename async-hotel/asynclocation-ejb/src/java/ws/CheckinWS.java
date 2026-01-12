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

            // ✅ EXACTEMENT comme dans checkin-saisie.jsp ligne 30
            ReservationDetailsCheck[] res = mere.getListeSansCheckIn("RESTSANSCIGROUPLIB_SANS", c);
            
            if (res == null || res.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("erreur", true);
                error.put("message", "Aucun détail de réservation trouvé");
                return Response.ok(error).build();
            }
            
            int nombreLigne = res.length;

            // ✅ EXACTEMENT comme dans checkin-saisie.jsp ligne 22-28
            ReservationLib t = new ReservationLib();
            t.setNomTable("RESERVATION_LIB_MIN_DATYF");
            ReservationLib[] liste = (ReservationLib[]) CGenUtil.rechercher(t, null, null, c, " AND ID='" + idresa + "'");
            boolean erreur = false;
            if (liste.length > 0) {
                if (liste[0].getResteAPayer() > 0) {
                    erreur = true;
                }
            }

            // ✅ Préparer les données exactement comme dans le JSP
            List<Map<String, Object>> lignes = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            
            // ✅ EXACTEMENT comme dans checkin-saisie.jsp ligne 94-109
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
            
            if (erreur) {
                result.put("message", "Le reste à payer de cette réservation n'est pas nul");
            }
            
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
    try {
        HttpSession session = request.getSession();
        user.UserEJB u = (user.UserEJB) session.getAttribute("u");
        if (u == null || u.getUser() == null) {
            return Response.status(401)
                .entity("{\"error\":\"Utilisateur non authentifié\"}")
                .build();
        }
        
        String idreservation = (String) data.get("idreservation");
        List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");

        reservation.CheckInLib[] cfille = new reservation.CheckInLib[lignes.size()];
        
        for (int i = 0; i < lignes.size(); i++) {
            Map<String, Object> ligne = lignes.get(i);
            
            reservation.CheckInLib checkin = new reservation.CheckInLib();
            
            checkin.setIdProduit((String) ligne.get("idproduit"));
            checkin.setIdClient((String) ligne.get("idclient"));
            checkin.setReservation((String) ligne.get("reservation"));
            checkin.setQte(Double.parseDouble(ligne.get("qte").toString()));
            checkin.setDaty(utilitaire.Utilitaire.string_date("dd/MM/yyyy", (String) ligne.get("daty")));
            
            // ✅ AJOUT : Référence produit (OBLIGATOIRE pour le CheckOut)
            if (ligne.get("referenceproduit") != null) {
                checkin.setRefproduit((String) ligne.get("referenceproduit"));
            }
            
            if (ligne.get("heure") != null && !ligne.get("heure").toString().isEmpty()) {
                checkin.setHeure((String) ligne.get("heure"));
            }
            
            if (ligne.get("responsable") != null) {
                checkin.setResponsable((String) ligne.get("responsable"));
            }
            
            if (ligne.get("image") != null) {
                checkin.setImage((String) ligne.get("image"));
            }
            
            if (ligne.get("magasin") != null) {
                checkin.setIdmagasin((String) ligne.get("magasin"));
            }
            
            checkin.setNomTable("CHECKIN");
            
            cfille[i] = checkin;
        }
        
        Reservation reservation = new Reservation();
        reservation.createObjectFilleMultipleSansMere(u, cfille);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Livraison enregistrée avec succès");
        return Response.ok(result).build();
        
    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", e.getMessage());
        return Response.serverError().entity(error).build();
    }
}

//     @POST
// @Path("/livraison-valider")
// public Response validerLivraison(Map<String, Object> data, @Context HttpServletRequest request) {
//     try {
//         HttpSession session = request.getSession();
//         user.UserEJB u = (user.UserEJB) session.getAttribute("u");
//         if (u == null || u.getUser() == null) {
//             return Response.status(401)
//                 .entity("{\"error\":\"Utilisateur non authentifié\"}")
//                 .build();
//         }
        
//         String idreservation = (String) data.get("idreservation");
//         List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");

//         // ✅ Utiliser CheckInLib[] au lieu de Check[]
//         reservation.CheckInLib[] cfille = new reservation.CheckInLib[lignes.size()];
        
//         for (int i = 0; i < lignes.size(); i++) {
//             Map<String, Object> ligne = lignes.get(i);
            
//             // ✅ Créer CheckInLib (qui a setImage(), setIdclientlib(), etc.)
//             reservation.CheckInLib checkin = new reservation.CheckInLib();
            
//             checkin.setIdProduit((String) ligne.get("idproduit"));
//             checkin.setIdClient((String) ligne.get("idclient"));
//             checkin.setReservation((String) ligne.get("reservation"));
//             checkin.setQte(Double.parseDouble(ligne.get("qte").toString()));
//             checkin.setDaty(utilitaire.Utilitaire.string_date("dd/MM/yyyy", (String) ligne.get("daty")));
            
//             if (ligne.get("heure") != null && !ligne.get("heure").toString().isEmpty()) {
//                 checkin.setHeure((String) ligne.get("heure"));
//             }
            
//             if (ligne.get("responsable") != null) {
//                 checkin.setResponsable((String) ligne.get("responsable"));
//             }
            
//             // ✅ Maintenant setImage() existe car CheckInLib l'a
//             if (ligne.get("image") != null) {
//                 checkin.setImage((String) ligne.get("image"));
//             }
            
//             if (ligne.get("magasin") != null) {
//                 checkin.setIdmagasin((String) ligne.get("magasin"));
//             }
            
//             // ✅ Changer nomTable APRÈS avoir rempli les données
//             checkin.setNomTable("CHECKIN");
            
//             cfille[i] = checkin;
//         }
        
//         // ✅ Comme CheckInLib hérite de Check, ça fonctionnera
//         Reservation reservation = new Reservation();
//         reservation.createObjectFilleMultipleSansMere(u, cfille);
        
//         Map<String, Object> result = new HashMap<>();
//         result.put("success", true);
//         result.put("message", "Livraison enregistrée avec succès");
//         return Response.ok(result).build();
        
//     } catch (Exception e) {
//         e.printStackTrace();
//         Map<String, Object> error = new HashMap<>();
//         error.put("success", false);
//         error.put("error", e.getMessage());
//         return Response.serverError().entity(error).build();
//     }
// }
}