<content:title>
    <fmt:message key="letters" /> (${fn:length(letters)})
</content:title>

<content:section cssId="letterListPage">
    <div class="section row">
        <a id="exportToCsvButton" class="right btn waves-effect waves-light grey-text white" 
           href="<spring:url value='/content/letter/list/letters.csv' />">
            <fmt:message key="export.to.csv" /><i class="material-icons right">vertical_align_bottom</i>
        </a>
        <script>
            $(function() {
                $('#exportToCsvButton').click(function() {
                    console.info('#exportToCsvButton click');
                    Materialize.toast('Preparing CSV file. Please wait...', 4000, 'rounded');
                });
            });
        </script>
        
        <p>
            <fmt:message key="to.add.new.content.click.the.button.below" />
        </p>
    
        <c:if test="${not empty letters}">
            <table class="bordered highlight">
                <thead>
                    <th><fmt:message key="frequency" /></th>
                    <th><fmt:message key="letter" /></th>
                    <th><fmt:message key="diacritic" /></th>
                    <th><fmt:message key="revision" /></th>
                </thead>
                <tbody>
                    <c:forEach var="letter" items="${letters}">
                        <tr class="letter">
                            <td>
                                ${letter.usageCount}<br />
                                <div class="progress">
                                    <div class="determinate" style="width: ${letter.usageCount * 100 / maxUsageCount}%"></div>
                                </div>
                            </td>
                            <td style="font-size: 2em;">
                                <a name="${letter.id}"></a>
                                <a href="<spring:url value='/content/letter/edit/${letter.id}' />">"<c:out value='${letter.text}' />"</a>
                            </td>
                            <td>
                                ${letter.diacritic}
                            </td>
                            <td>
                                #${letter.revisionNumber}
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>
    
    <div class="fixed-action-btn" style="bottom: 2em; right: 2em;">
        <a href="<spring:url value='/content/letter/create' />" class="btn-floating btn-large tooltipped" data-position="left" data-delay="50" data-tooltip="<fmt:message key="add.letter" />"><i class="material-icons">add</i></a>
    </div>
</content:section>
