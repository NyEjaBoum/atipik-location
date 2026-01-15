package ws;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bean.CGenUtil;
import proforma.ProformaLib;
import proforma.Proforma;
import reservation.Reservation;
import proforma.ProformaDetails;
import proforma.ProformaDetailsLib;

import utilitaire.UtilDB;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import produits.IngredientsLib;

@Path("/proforma")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProformaWS {


// ...existing code...

    @GET
    @Path("/produits-disponibles")
    public Response getProduitsDisponibles(
            @QueryParam("dateDebut") String dateDebut,
            @QueryParam("dateFin") String dateFin,
            @Context HttpServletRequest request) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            // Vérifier que les dates sont fournies
            if (dateDebut == null || dateDebut.isEmpty() || dateFin == null || dateFin.isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\":\"dateDebut et dateFin sont obligatoires (format: YYYY-MM-DD)\"}")
                    .build();
            }
            
            // Calculer le nombre de jours entre les deux dates
            java.sql.Date debut = java.sql.Date.valueOf(dateDebut);
            java.sql.Date fin = java.sql.Date.valueOf(dateFin);
            long nbJours = (fin.getTime() - debut.getTime()) / (1000 * 60 * 60 * 24);
            
            // Récupérer tous les produits (non endommagés)
            IngredientsLib crit = new IngredientsLib();
            crit.setNomTable("ST_INGREDIENTSAUTOVENTE_MIMAGE");
            
            IngredientsLib[] produits = (IngredientsLib[]) CGenUtil.rechercher(crit, null, null, c, "");
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (IngredientsLib p : produits) {
                // Vérifier la disponibilité du produit pour chaque jour de la période
                boolean disponible = true;
                double qteDisponibleMin = Double.MAX_VALUE;
                
                for (long i = 0; i <= nbJours; i++) {
                    java.sql.Date dateTest = new java.sql.Date(debut.getTime() + (i * 24 * 60 * 60 * 1000));
                    
                    // Utiliser la méthode checkDisponibilite de la classe Ingredients
                    produits.Ingredients ing = new produits.Ingredients();
                    ing.setId(p.getId());
                    produits.Ingredients[] ings = (produits.Ingredients[]) CGenUtil.rechercher(ing, null, null, c, "");
                    
                    if (ings != null && ings.length > 0) {
                        double qteDispo = ings[0].checkDisponibilite(c, dateTest);
                        if (qteDispo < qteDisponibleMin) {
                            qteDisponibleMin = qteDispo;
                        }
                        if (qteDispo <= 0) {
                            disponible = false;
                            break;
                        }
                    } else {
                        disponible = false;
                        break;
                    }
                }
                
                // Ajouter le produit uniquement s'il est disponible sur toute la période
                if (disponible && qteDisponibleMin > 0) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("libelle", p.getLibelle());
                    map.put("reference", p.getReference());
                    // ✅ FIX : Utiliser le bon nom de méthode
                    map.put("categorie", p.getCategorieIngredient()); // Majuscule sur 'I'
                    map.put("pu", p.getPu());
                    map.put("unite", p.getUnite());
                    map.put("image", p.getImage());
                    map.put("dateDebut", dateDebut);
                    map.put("dateFin", dateFin);
                    map.put("nbJours", nbJours);
                    map.put("qteDisponible", (int) qteDisponibleMin);
                    result.add(map);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("total", result.size());
            response.put("dateDebut", dateDebut);
            response.put("dateFin", dateFin);
            response.put("nbJours", nbJours);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ex) {}
        }
    }

// ...existing code...

@POST
@Path("/valider/{id}")
@Consumes(MediaType.APPLICATION_JSON)
public Response validerProforma(@PathParam("id") String id, @Context HttpServletRequest request) {
    Connection c = null;
    try {
        HttpSession session = request.getSession();
        user.UserEJB u = (user.UserEJB) session.getAttribute("u");
        if (u == null || u.getUser() == null) {
            return Response.status(401).entity("{\"error\":\"Utilisateur non authentifié\"}").build();
        }
        String userId = u.getUser().getTuppleID();

        c = new UtilDB().GetConn();
        c.setAutoCommit(false);

        // ✅ Utiliser la même logique que apresValiderProforma.jsp
        Proforma p = new Proforma();
        p.setId(id);
        
        // Valider la proforma (crée la BC et la réservation)
        Proforma proforma = (Proforma) p.validerObject(userId);
        
        c.commit();

        // ✅ Récupérer la réservation créée et la vente générée
        Reservation reservation = proforma.getReservationPF();
        if (reservation == null) {
            return Response.status(400)
                .entity("{\"error\":\"Aucune réservation créée\"}")
                .build();
        }

        vente.Vente vente = reservation.getVente(c);
        if (vente == null) {
            return Response.status(400)
                .entity("{\"error\":\"Aucune vente créée\"}")
                .build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("proformaId", proforma.getId());
        response.put("reservationId", reservation.getId());
        response.put("venteId", vente.getId());
        response.put("message", "Proforma validée et BC créée avec succès");
        
        return Response.ok(response).build();

    } catch (Exception e) {
        if (c != null) try { c.rollback(); } catch (Exception ignore) {}
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", e.getMessage());
        error.put("type", e.getClass().getSimpleName());
        return Response.serverError().entity(error).build();
    } finally {
        if (c != null) try { c.close(); } catch (Exception ignore) {}
    }
}

@GET
@Path("/fiche/{id}")
public Response ficheProforma(@PathParam("id") String id) {
    Connection c = null;
    try {
        c = new UtilDB().GetConn();
        ProformaLib p = new ProformaLib();
        p.setId(id);
        p = (ProformaLib) p.getById(id, "PROFORMA_CPL", c);
        if (p == null) {
            return Response.status(404).entity("{\"error\":\"Proforma introuvable\"}").build();
        }

        Map<String, Object> fiche = new HashMap<>();
        fiche.put("id", p.getId());
        fiche.put("designation", p.getDesignation());
        fiche.put("magasin", p.getIdMagasinLib());
        fiche.put("date", p.getDaty() != null ? p.getDaty().toString() : null);
        fiche.put("client", p.getIdClientLib());
        fiche.put("etat", p.getEtatLib());
        fiche.put("devise", "AR"); // ou p.getDevise() si tu as le getter
        fiche.put("remarque", p.getRemarque());
        fiche.put("montantSansRemise", p.getMontant());
        fiche.put("montantRemise", p.getMontantremise());
        fiche.put("montantRestant", p.getMontantreste());
        fiche.put("remise", p.getRemise());
        fiche.put("periode", p.getPeriode());
        fiche.put("etatPayment", p.getEtatpaymentlib() != null ? p.getEtatpaymentlib() : "");
        fiche.put("lieuLocation", p.getLieulocation());

        // Utilisation de PROFORMADETAILS_CPL et ProformaDetailsLib
        ProformaDetailsLib crit = new ProformaDetailsLib();
        crit.setNomTable("PROFORMADETAILS_CPL");
        crit.setIdProforma(p.getId());
        ProformaDetailsLib[] details = (ProformaDetailsLib[]) CGenUtil.rechercher(crit, null, null, c, "");

        List<Map<String, Object>> detailsList = new ArrayList<>();
        if (details != null) {
            for (ProformaDetailsLib d : details) {
                Map<String, Object> det = new HashMap<>();
                det.put("id", d.getId());
                det.put("produit", d.getIdProduit());
                det.put("produitLib", d.getIdProduitLib());
                det.put("designation", d.getDesignation());
                det.put("image", d.getImage());
                det.put("quantiteArticle", d.getNombre());
                det.put("quantiteJour", d.getQte());
                det.put("dateDebut", d.getDatedebut() != null ? d.getDatedebut().toString() : null);
                det.put("prixUnitaire", d.getPu());
                det.put("puTotal", d.getPuTotal());
                det.put("uniteLib", d.getUniteLib());
                det.put("montantRemise", d.getRemisemontant());
                det.put("montantTotal", d.getMontanttotal());
                double montant = (d.getPu() * d.getQte()) - d.getRemisemontant();
                det.put("montant", montant);
                det.put("montantTva", d.getMontanttva());
                det.put("montantTtc", d.getMontantttc());
                detailsList.add(det);
            }
        }
        fiche.put("details", detailsList);

        return Response.ok(fiche).build();
    } catch (Exception e) {
        e.printStackTrace();
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return Response.serverError().entity(error).build();
    } finally {
        if (c != null) try { c.close(); } catch (Exception ex) {}
    }
}
    @GET
    @Path("/liste")
    public Response listeProformas(
            @QueryParam("dateMin") String dateMin,
            @QueryParam("dateMax") String dateMax,
            @QueryParam("idClient") String idClient,
            @QueryParam("etat") Integer etat,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("npp") @DefaultValue("10") int npp) {

        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            ProformaLib proforma = new ProformaLib();
            proforma.setNomTable("PROFORMA_CPL");

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
            if (etat != null) {
                where.append(" AND etat = ").append(etat);
            }

            ProformaLib[] proformas = (ProformaLib[]) CGenUtil.rechercher(
                proforma, null, null, c, where.toString()
            );

            List<Map<String, Object>> result = new ArrayList<>();
            double sommeMontant = 0;
            double sommeMontantPaye = 0;
            double sommeMontantReste = 0;

            if (proformas != null) {
                for (ProformaLib p : proformas) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("daty", p.getDaty() != null ? p.getDaty().toString() : null);
                    item.put("idClient", p.getIdClient());
                    item.put("idClientLib", p.getIdClientLib());
                    item.put("idMagasin", p.getIdMagasin());
                    item.put("idMagasinLib", p.getIdMagasinLib());
                    item.put("montant", p.getMontantTtc());
                    item.put("montantPaye", p.getMontantPaye());
                    item.put("montantReste", p.getMontantreste());
                    item.put("etat", p.getEtat());
                    item.put("etatLib", p.getEtatLib());
                    item.put("designation", p.getDesignation());
                    item.put("remarque", p.getRemarque());

                    sommeMontant += p.getMontantTtc();
                    sommeMontantPaye += p.getMontantPaye();
                    sommeMontantReste += p.getMontantreste();

                    result.add(item);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", result);
            response.put("total", result.size());
            response.put("sommeMontant", sommeMontant);
            response.put("sommeMontantPaye", sommeMontantPaye);
            response.put("sommeMontantReste", sommeMontantReste);

            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ex) {}
        }
    }

    @POST
    @Path("/creer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response creerProforma(Map<String, Object> data, @Context HttpServletRequest request) {
        Connection c = null;
        try {
            HttpSession session = request.getSession();
            user.UserEJB u = (user.UserEJB) session.getAttribute("u");
            
            // Authentification alternative via header X-User-Id (pour appels API externes comme nouveau-front)
            String apiUserId = request.getHeader("X-User-Id");
            if (u == null && (apiUserId == null || apiUserId.isEmpty())) {
                return Response.status(401).entity("{\"error\":\"Utilisateur non authentifié\"}").build();
            }
            
            // Si pas de session mais header API présent, on continue (appel depuis nouveau-front)
            String utilisateurId = (u != null) ? "session" : apiUserId;
            System.out.println("ProformaWS.creerProforma - Utilisateur: " + utilisateurId);
            
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            // ✅ 1. Créer la proforma mère avec le bon nom de table
            Proforma mere = new Proforma();
            // Ne pas changer le nomTable - laisser le constructeur par défaut "PROFORMA"
            
            // ✅ 2. Remplir les données
            if (data.get("daty") != null) {
                mere.setDaty(java.sql.Date.valueOf((String)data.get("daty")));
            } else {
                mere.setDaty(new java.sql.Date(System.currentTimeMillis()));
            }
            if (data.get("idClient") != null) {
                mere.setIdClient((String)data.get("idClient"));
            }
            if (data.get("idMagasin") != null) {
                mere.setIdMagasin((String)data.get("idMagasin"));
            }
            if (data.get("remarque") != null) {
                mere.setRemarque((String)data.get("remarque"));
            }
            if (data.get("remise") != null) {
                mere.setRemise(Double.parseDouble(data.get("remise").toString()));
            }
            if (data.get("designation") != null) {
                mere.setDesignation((String)data.get("designation"));
            }
            if (data.get("lieuLocation") != null) {
                mere.setLieulocation((String)data.get("lieuLocation"));
            }
            if (data.get("dateDebutReservation") != null) {
                mere.setDateprevres(java.sql.Date.valueOf((String)data.get("dateDebutReservation")));
            }
            if (data.get("caution") != null) {
                mere.setCaution(Double.parseDouble(data.get("caution").toString()));
            }
            
            mere.setEtat(data.get("etat") != null ? convertToInt(data.get("etat")) : 0);
            mere.setTva(0);
            
            // ✅ 3. Créer les détails
            List<Map<String, Object>> detailsData = (List<Map<String, Object>>) data.get("details");
            if (detailsData == null || detailsData.isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\":\"Aucun détail fourni\"}")
                    .build();
            }
            
            ProformaDetails[] filles = new ProformaDetails[detailsData.size()];
            
            for (int i = 0; i < detailsData.size(); i++) {
                Map<String, Object> d = detailsData.get(i);
                ProformaDetails pd = new ProformaDetails();
                // Le constructeur définit déjà le bon nomTable "PROFORMA_DETAILS"
                
                String idProduit = d.get("idProduit") != null ? (String)d.get("idProduit") : "";
                if (idProduit.isEmpty()) {
                    return Response.status(400)
                        .entity("{\"error\":\"idProduit manquant pour le détail " + (i+1) + "\"}")
                        .build();
                }
                pd.setIdProduit(idProduit);
                
                pd.setDesignation(d.get("designation") != null ? (String)d.get("designation") : "");
                pd.setNombre(d.get("nombre") != null ? convertToInt(d.get("nombre")) : 1);
                pd.setQte(d.get("qte") != null ? convertToInt(d.get("qte")) : 1);
                pd.setPu(d.get("pu") != null ? Double.parseDouble(d.get("pu").toString()) : 0.0);
                pd.setUnite(d.get("unite") != null ? (String)d.get("unite") : "");
                pd.setIdDevise("AR");
                pd.setTauxDeChange(1);
                
                if (d.get("dateDebut") != null) {
                    pd.setDatedebut(java.sql.Date.valueOf((String)d.get("dateDebut")));
                }
                
                pd.setTva(0);
                pd.setRemise(0);
                pd.setMargemoins(d.get("margemoins") != null ? convertToInt(d.get("margemoins")) : 1);
                pd.setMargeplus(d.get("margeplus") != null ? convertToInt(d.get("margeplus")) : 1);
                
                filles[i] = pd;
            }
            
            // ✅ 4. Attacher les filles à la mère
            mere.setFille(filles);
            
            // ✅ 5. Utiliser createObject qui gère tout (caution, détails, etc.)
            String userId;
            if (u != null && u.getUser() != null) {
                userId = u.getUser().getTuppleID();
            } else if (apiUserId != null && !apiUserId.isEmpty()) {
                userId = apiUserId;
            } else {
                userId = "ANONYMOUS";
            }
            Proforma created = (Proforma) mere.createObject(userId, c);
            
            c.commit();
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("id", created.getId());
            resp.put("message", "Proforma créée avec succès");
            return Response.ok(resp).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            if (c != null) try { c.rollback(); } catch (Exception ex) {}
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            error.put("details", e.getClass().getSimpleName());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ex) {}
        }
    }

    private int convertToInt(Object value) {
        if (value == null) return 0;
        
        String str = value.toString()
            .replace(",", ".")
            .replace(" ", "")
            .trim();
        
        if (str.isEmpty()) return 0;
        
        try {
            return (int) Math.floor(Double.parseDouble(str));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @GET
    @Path("/clients")
    public Response getClients(@Context HttpServletRequest request) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            client.Client crit = new client.Client();
            crit.setNomTable("CLIENT");
            client.Client[] clients = (client.Client[]) CGenUtil.rechercher(crit, null, null, c, "");
            
            List<Map<String, String>> result = new ArrayList<>();
            for (client.Client cl : clients) {
                Map<String, String> map = new HashMap<>();
                map.put("id", cl.getId());
                map.put("nom", cl.getNom());
                result.add(map);
            }
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ex) {}
        }
    }

    @GET
    @Path("/produits")
    public Response getProduits(@Context HttpServletRequest request) {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            // ✅ FIX : Utilisation correcte de IngredientsLib
            IngredientsLib crit = new IngredientsLib();
            crit.setNomTable("ST_INGREDIENTSAUTOVENTE_MIMAGE");
            
            IngredientsLib[] produits = (IngredientsLib[]) CGenUtil.rechercher(crit, null, null, c, "");
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (IngredientsLib p : produits) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("libelle", p.getLibelle());
                map.put("pu", p.getPu());
                map.put("unite", p.getUnite());
                map.put("compte_vente", p.getCompte_vente());
                map.put("tva", p.getTva());
                map.put("image", p.getImage());
                result.add(map);
            }
            return Response.ok(result).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return Response.serverError().entity(error).build();
        } finally {
            if (c != null) try { c.close(); } catch (Exception ex) {}
        }
    }
}