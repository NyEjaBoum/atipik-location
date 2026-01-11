package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import vente.VenteLib;
import vente.VenteDetailsLib;
import utilitaire.UtilDB;
import bean.CGenUtil;

@Path("/vente")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VenteWS {

    @GET
    @Path("/fiche/{id}")
    public Response ficheVente(@PathParam("id") String id) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            // Récupérer la vente
            VenteLib vente = new VenteLib();
            vente.setId(id);
            vente = (VenteLib) vente.getById(id, "VENTE_CPL", c);

            if (vente == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Vente introuvable");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            // Construire la fiche
            Map<String, Object> fiche = new HashMap<>();
            fiche.put("id", vente.getId());
            fiche.put("designation", vente.getDesignation());
            fiche.put("remarque", vente.getRemarque());
            fiche.put("magasin", vente.getIdMagasinLib());
            fiche.put("date", vente.getDaty() != null ? vente.getDaty().toString() : null);
            fiche.put("client", vente.getIdClientLib());
            fiche.put("telephone", vente.getTelephone());
            fiche.put("etat", vente.getEtat()); // Correction ici : getEtat() au lieu de getEtatlib()
            fiche.put("montant", vente.getMontanttotal());
            fiche.put("montantPaye", vente.getMontantpaye());
            fiche.put("montantRemise", vente.getMontantRemise());
            fiche.put("montantReste", vente.getMontantreste());
            fiche.put("datePrevue", vente.getDatyprevu() != null ? vente.getDatyprevu().toString() : null);
            fiche.put("periode", vente.getPeriode());
            fiche.put("idReservation", vente.getIdReservation());
            fiche.put("etatLogistique", vente.getEtatlogistique()); // Idem : getEtatlogistique() si la méthode existe
            fiche.put("idDevise", vente.getIdDevise());
            fiche.put("tauxDeChange", vente.getTauxdechange());

            // Récupérer les détails de la vente
            VenteDetailsLib vdLib = new VenteDetailsLib();
            vdLib.setNomTable("VENTE_DETAILS_CPL");
            vdLib.setIdVente(id);
            VenteDetailsLib[] details = (VenteDetailsLib[]) CGenUtil.rechercher(vdLib, null, null, c, "");

            List<Map<String, Object>> detailsList = new ArrayList<>();
            for (VenteDetailsLib detail : details) {
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("id", detail.getId());
                detailMap.put("designation", detail.getDesignation());
                detailMap.put("idProduitLib", detail.getIdProduitLib());
                detailMap.put("quantite", detail.getQte());
                detailMap.put("prixUnitaire", detail.getPu());
                detailMap.put("montantAvantRemise", detail.getMontantAvantRemise());
                detailMap.put("montantRemise", detail.getRemisemontant());
                detailMap.put("montant", detail.getMontant());
                detailMap.put("image", detail.getImage());
                detailMap.put("reference", detail.getReference());
                detailsList.add(detailMap);
            }

            fiche.put("details", detailsList);

            // Calculer les totaux
            double montantTotal = 0;
            double montantRemiseTotal = 0;
            double montantFinal = 0;
            for (VenteDetailsLib detail : details) {
                montantTotal += detail.getMontantAvantRemise();
                montantRemiseTotal += detail.getRemisemontant();
                montantFinal += detail.getMontant();
            }
            fiche.put("montantTotalDetails", montantTotal);
            fiche.put("montantRemiseDetails", montantRemiseTotal);
            fiche.put("montantFinalDetails", montantFinal);

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
}