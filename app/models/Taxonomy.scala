package models

/** A simple category taxonomy for the API **/
object Taxonomy {
  
  private val taxonomy = 
    Map("Texts" 
          -> Seq("Inscriptions", 
                 "Literary Texts"),
                 
        "Archaeology" 
          -> Seq("Artefacts",
                 "Sites"),
                 
        "Numismatics" -> Seq.empty[String], // no sub-categories
        
        "Maps" -> Seq.empty[String],        // no sub-categories
        
        "Images" 
          -> Seq("Drawings",
                 "Photos"),
        
        "Scholarship" -> Seq.empty[String]  // no sub-categories
    )
    
  /** The top level terms, in alphabetical order **/
  lazy val topLevelTerms = taxonomy.keys.toSeq.sorted
    
  /** Gets the sub-categories for the term, if any (and if the term exists) **/
  def getSubCategories(term: String): Option[Seq[String]] =
    taxonomy.get(term)

  def getPaths(tags: Seq[String]): Seq[Seq[String]] = {
    def normalize(term: String) = { if (term.endsWith("s")) term.substring(0, term.size - 1) else term }.toLowerCase
      
    val normalizedTags = tags.map(normalize(_)).toSet
    
    val matchedSingleLevelCategories = taxonomy
      .filter(_._2.isEmpty)
      .map(_._1)
      .filter(category => normalizedTags.contains(normalize(category)))
      .toSeq
      .map(Seq(_))
      
    val matchedSecondLevelCategories = taxonomy
      .mapValues(_.filter(subcategory => normalizedTags.contains(normalize(subcategory))))
      .filter(_._2.size > 0)
      .toSeq
      .flatMap { case (topCategory, subcategories) => subcategories.map(Seq(topCategory, _))}
        
    matchedSecondLevelCategories ++ matchedSingleLevelCategories
  }
  
}