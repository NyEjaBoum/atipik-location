package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.*;

import bean.CGenUtil;
import reservation.ReservationLib;
import reservation.ReservationDetailsLib;
import caution.CautionLib;
import utilitaire.UtilDB;

@Path("/reservation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReservationWS {


@GET
@Path("/fiche/{id}")
public Response ficheReservation(@PathParam("id") String id) {
    Connection c = null;
    try {
        c = new UtilDB().GetConn();
        
        // Récupérer la réservation (utiliser RESERVATION_LIB comme dans le JSP)
        ReservationLib resa = new ReservationLib();
        resa.setId(id);
        resa = (ReservationLib) resa.getById(id, "RESERVATION_LIB", c);
        
        if (resa == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Réservation introuvable");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Construire la fiche
        Map<String, Object> fiche = new HashMap<>();
        fiche.put("id", resa.getId());
        fiche.put("client", resa.getIdclientlib());
        fiche.put("date", resa.getDaty() != null ? resa.getDaty().toString() : null);
        fiche.put("remarque", resa.getRemarque());
        fiche.put("etat", resa.getEtatlib());
        fiche.put("montant", resa.getMontant());
        fiche.put("montantTotal", resa.getMontanttotal());
        fiche.put("montantTva", resa.getMontantTva());
        fiche.put("montantTTC", resa.getMontantTTC());
        fiche.put("montantCaution", resa.getMontantcaution());
        fiche.put("montantRemise", resa.getMontantremise());
        fiche.put("paye", resa.getPaye());
        fiche.put("resteAPayer", resa.getResteAPayer());
        fiche.put("lieulocation", resa.getLieulocation());
        fiche.put("periode", resa.getPeriode());
        fiche.put("numBl", resa.getNumBl());
        fiche.put("idorigine", resa.getIdorigine());
        fiche.put("datePrevisionRetour", resa.getDatePrevisionRetour() != null ? resa.getDatePrevisionRetour().toString() : null);
        fiche.put("datePrevisionDepart", resa.getDatePrevisionDepart() != null ? resa.getDatePrevisionDepart().toString() : null);

        // Récupérer les détails avec la même connexion
        ReservationDetailsLib crit = new ReservationDetailsLib();
        crit.setNomTable("RESERVATIONDETAILS_LIB_MARGE");
        crit.setIdmere(id);
        ReservationDetailsLib[] details = (ReservationDetailsLib[]) CGenUtil.rechercher(crit, null, null, c, "");

        List<Map<String, Object>> detailsList = new ArrayList<>();
        if (details != null) {
            for (ReservationDetailsLib d : details) {
                Map<String, Object> det = new HashMap<>();
                det.put("id", d.getId());
                det.put("produit", d.getLibelleproduit());
                det.put("reference", d.getReferenceproduit());
                det.put("image", d.getImage());
                det.put("qtearticle", d.getQtearticle());
                det.put("qte", d.getQte());
                det.put("pu", d.getPu());
                det.put("montant", d.getMontant());
                det.put("date", d.getDaty() != null ? d.getDaty().toString() : null);
                detailsList.add(det);
            }
        }
        fiche.put("details", detailsList);

        // Récupérer la caution avec la même connexion
        CautionLib cautionCrit = new CautionLib();
        cautionCrit.setIdreservation(id);
        CautionLib[] cautions = (CautionLib[]) CGenUtil.rechercher(cautionCrit, null, null, c, "");
        CautionLib caution = (cautions != null && cautions.length > 0) ? cautions[0] : null;

        if (caution != null) {
            fiche.put("cautionId", caution.getId());
            fiche.put("datyCaution", caution.getDaty() != null ? caution.getDaty().toString() : null);
            fiche.put("showReglerCaution", caution.getDaty() == null);
        } else {
            fiche.put("cautionId", null);
            fiche.put("datyCaution", null);
            fiche.put("showReglerCaution", false);
        }

        return Response.ok(fiche).build();
        
    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", e.getMessage());
        return Response.serverError().entity(error).build();
    } finally {
        if (c != null) try { c.close(); } catch (Exception ignore) {}
    }
}

    @GET
    @Path("/liste")
    public Response listeReservations(
            @QueryParam("dateMin") String dateMin,
            @QueryParam("dateMax") String dateMax,
            @QueryParam("idClient") String idClient,
            @QueryParam("etatPayment") Integer etatPayment,
            @QueryParam("etatLogistique") Integer etatLogistique,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("npp") @DefaultValue("10") int npp
    ) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            ReservationLib crit = new ReservationLib();
            crit.setNomTable("RESERVATION_ETATLIB_F");

            StringBuilder where = new StringBuilder();
            if (dateMin != null && !dateMin.isEmpty()) {
                where.append(" AND daty >= TO_DATE('").append(dateMin).append("', 'YYYY-MM-DD')");
            }
            if (dateMax != null && !dateMax.isEmpty()) {
                where.append(" AND daty <= TO_DATE('").append(dateMax).append("', 'YYYY-MM-DD')");
            }
            if (idClient != null && !idClient.isEmpty()) {
                where.append(" AND idclient = '").append(idClient).append("'");
            }
            if (etatPayment != null) {
                where.append(" AND etatpayment = ").append(etatPayment);
            }
            if (etatLogistique != null) {
                where.append(" AND etatlogistique = ").append(etatLogistique);
            }

            ReservationLib[] reservations = (ReservationLib[]) CGenUtil.rechercher(
                crit, null, null, c, where.toString()
            );

            List<Map<String, Object>> result = new ArrayList<>();
            double sommeMontant = 0;

            if (reservations != null) {
                for (ReservationLib r : reservations) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("client", r.getIdclientlib());
                    map.put("date", r.getDaty() != null ? r.getDaty().toString() : null);
                    map.put("remarque", r.getRemarque());
                    map.put("etatPayment", r.getEtatpaymentlib());
                    map.put("etatLogistique", r.getEtatlogistiquelib());
                    map.put("montant", r.getMontant());
                    result.add(map);
                    sommeMontant += r.getMontant();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("total", result.size());
            response.put("sommeMontant", sommeMontant);

            return Response.ok(response).build();

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ignore) {}
        }
    }
}